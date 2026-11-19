"""
艾宾浩斯记忆助手 - 本地服务器

功能：
1. 托管静态文件（index.html / sw.js / manifest.json 等）
2. 提供 /api/ai 代理，把前端的流式请求转发给智谱大模型

为什么要代理：
浏览器从 http://localhost:8080（或 file://）直接 POST https://open.bigmodel.cn
会先发跨域预检请求（OPTIONS），而智谱网关对预检不返回 Access-Control-Allow-*
响应头，浏览器因此直接抛 TypeError: Failed to fetch。
走同源的 /api/ai 就不存在跨域问题。
"""

import http.server
import os
import sys
import json
import urllib.request
import urllib.error
import webbrowser

PORT = 8080

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
os.chdir(SCRIPT_DIR)

AI_ENDPOINT = 'https://open.bigmodel.cn/api/paas/v4/chat/completions'
# 服务端不内置 Key：默认由前端在请求头里带（设置页填写），也支持环境变量兜底
AI_API_KEY = os.environ.get('AI_API_KEY', '').strip()
AI_MODELS = ['glm-4-flash', 'glm-4-flash-250414']

CORS_HEADERS = [
    ('Access-Control-Allow-Origin', '*'),
    ('Access-Control-Allow-Methods', 'POST, GET, OPTIONS'),
    ('Access-Control-Allow-Headers', 'Content-Type, Authorization'),
]


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = dict(http.server.SimpleHTTPRequestHandler.extensions_map)
    extensions_map.update({
        '.html': 'text/html',
        '.js': 'application/javascript',
        '.json': 'application/json',
        '.css': 'text/css',
        '.svg': 'image/svg+xml',
    })

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))

    def _send_cors(self):
        for k, v in CORS_HEADERS:
            self.send_header(k, v)

    def do_OPTIONS(self):
        self.send_response(200)
        self._send_cors()
        self.send_header('Content-Length', '0')
        self.end_headers()

    def do_POST(self):
        path = self.path.split('?')[0]
        if path != '/api/ai':
            self.send_error(404, 'Not Found')
            return

        try:
            length = int(self.headers.get('Content-Length') or 0)
            raw = self.rfile.read(length) if length > 0 else b'{}'
            payload = json.loads(raw.decode('utf-8') or '{}')
        except Exception as exc:
            self._send_json(400, {'error': {'message': '请求体解析失败: %s' % exc}})
            return

        payload.setdefault('stream', True)

        # 前端可以在请求头里指定服务商（设置页切换 AI 用），缺省走内置智谱配置
        endpoint = (self.headers.get('X-AI-Endpoint') or '').strip() or AI_ENDPOINT
        if not endpoint.startswith(('http://', 'https://')):
            self._send_json(400, {'error': {'message': '接口地址必须以 http(s):// 开头'}})
            return
        api_key = (self.headers.get('X-AI-Key') or '').strip() or AI_API_KEY
        if not api_key:
            self._send_json(400, {'error': {'message': '缺少 API Key，请先在页面「设置 → AI 配置」里填写'}})
            return
        wanted = (self.headers.get('X-AI-Model') or '').strip() or payload.get('model')

        models = [wanted] if wanted else []
        # 只有智谱有免费模型降级链，其它服务商不追加
        if 'bigmodel.cn' in endpoint:
            for m in AI_MODELS:
                if m not in models:
                    models.append(m)

        last_error = None
        for model in models:
            payload['model'] = model
            body = json.dumps(payload, ensure_ascii=False).encode('utf-8')
            req = urllib.request.Request(endpoint, data=body, method='POST', headers={
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream',
                'Authorization': 'Bearer ' + api_key,
            })
            try:
                resp = urllib.request.urlopen(req, timeout=120)
            except urllib.error.HTTPError as exc:
                err_body = b''
                try:
                    err_body = exc.read()
                except Exception:
                    pass
                last_error = (exc.code, err_body)
                if self._is_retryable(err_body):
                    sys.stderr.write('[ai] %s 不可用，尝试下一个模型\n' % model)
                    continue
                self._send_json(exc.code, self._decode_error(err_body))
                return
            except Exception as exc:
                self._send_json(502, {'error': {'message': '连接智谱接口失败: %s' % exc}})
                return

            self.send_response(200)
            self._send_cors()
            self.send_header('Content-Type', 'text/event-stream; charset=utf-8')
            self.send_header('Cache-Control', 'no-cache')
            self.send_header('X-AI-Model', model)
            self.end_headers()
            try:
                while True:
                    chunk = resp.read1(1024) if hasattr(resp, 'read1') else resp.read(1024)
                    if not chunk:
                        break
                    self.wfile.write(chunk)
                    self.wfile.flush()
            except (BrokenPipeError, ConnectionResetError):
                pass
            except Exception as exc:
                sys.stderr.write('[ai] 转发中断: %s\n' % exc)
            finally:
                try:
                    resp.close()
                except Exception:
                    pass
            return

        # 所有模型都失败
        if last_error:
            code, err_body = last_error
            self._send_json(code if code >= 400 else 502, self._decode_error(err_body))
        else:
            self._send_json(500, {'error': {'message': '没有可用的模型'}})

    def _is_retryable(self, err_body):
        """模型不存在 / 限流 时换下一个模型再试；鉴权与欠费直接返回"""
        text = err_body.decode('utf-8', 'ignore') if isinstance(err_body, bytes) else str(err_body)
        return ('1211' in text) or ('模型不存在' in text) or ('1302' in text) or ('1305' in text)

    def _decode_error(self, err_body):
        try:
            return json.loads(err_body.decode('utf-8'))
        except Exception:
            return {'error': {'message': err_body.decode('utf-8', 'ignore')[:500] or '未知错误'}}

    def _send_json(self, status, obj):
        data = json.dumps(obj, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self._send_cors()
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        try:
            self.wfile.write(data)
        except (BrokenPipeError, ConnectionResetError):
            pass


def start_server():
    httpd = http.server.ThreadingHTTPServer(("", PORT), Handler)
    print("=" * 50)
    print("  艾宾浩斯记忆助手 - 本地服务器")
    print("=" * 50)
    print("")
    print("  本地地址: http://localhost:%d" % PORT)
    print("  AI 代理:   http://localhost:%d/api/ai" % PORT)
    print("")
    print("  请用 http(s) 地址访问，不要直接双击 index.html")
    print("  （本地文件调用 AI 会被浏览器跨域策略拦截）")
    print("")
    print("  按 Ctrl+C 停止服务器")
    print("=" * 50)

    if not os.environ.get('NO_BROWSER'):
        webbrowser.open('http://localhost:%d' % PORT)

    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n服务器已停止")
    finally:
        httpd.server_close()


if __name__ == "__main__":
    start_server()

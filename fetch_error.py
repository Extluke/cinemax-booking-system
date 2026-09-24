import urllib.request, urllib.error
import sys

req = urllib.request.Request('http://localhost:8081/login-process', data=b'email=admin%40cineplex.com&password=admin123')
try:
    with urllib.request.urlopen(req) as response:
        cookie = response.headers.get('Set-Cookie')
except urllib.error.HTTPError as e:
    cookie = e.headers.get('Set-Cookie')

if not cookie:
    print("Login failed")
    sys.exit(1)

req2 = urllib.request.Request('http://localhost:8081/admin/manajemen-kursi?studioId=1')
req2.add_header('Cookie', cookie)
try:
    with urllib.request.urlopen(req2) as response:
        print("Success:", response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print("Error:", e.code)
    print(e.read().decode('utf-8'))

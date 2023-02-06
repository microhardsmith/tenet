import socket
import time

# 连接指定tcp服务端并定时发送消息
def start(port):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('successfully created socket')
    serverAddr = ('127.0.0.1', port)
    s.connect(serverAddr)
    print('successfully connecting server')
    try:
        while True:
            s.send(b'hello')
            time.sleep(5)
    except KeyboardInterrupt:
        s.close()
        exit()

if __name__ == '__main__':
    print('Start')
    start(19999)
    

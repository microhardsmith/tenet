import socket
import time

# 连接指定tcp服务端并定时发送消息
def start(serverPort):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(('127.0.0.1', serverPort))
    s.listen(5)
    try:
        conn,addr = s.accept()
        with conn:
            print(f"Connected by {addr}")
            while True:
                data = conn.recv(1024)
                if not data:
                    break
                print('data : ' + str(data))
    except KeyboardInterrupt:
        print('exit now ...')
        s.close()
        exit(0)                

if __name__ == '__main__':
    print('Start server')
    start(10705)
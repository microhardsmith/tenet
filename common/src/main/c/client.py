import socket
import time

# 连接指定tcp服务端并定时发送消息
def start(clientPort, serverPort):
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print('successfully created socket')

    clientAddr = ('127.0.0.1', clientPort)
    s.bind(clientAddr)

    serverAddr = ('127.0.0.1', serverPort)
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
    start(29999, 10705)
    

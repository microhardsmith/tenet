#ifndef LIB_MACOS_H
#define LIB_MACOS_H

FILE* g_stdout();

FILE* g_stderr();

int m_connect_block_code();

int m_send_block_code();

int m_kqueue();

int m_kevent_ctl(int kq, int fd, int16_t filter, uint16_t flags);

int m_kevent_wait(int kq, struct kevent* eventlist, int nevents, struct timespec* timeout);

socklen_t m_address_len();

int m_address(struct sockaddr_in* sockAddr, char* addrStr, socklen_t len);

uint16_t m_port(struct sockaddr_in* sockAddr);

int m_socket_create();

int m_accept(int socket, struct sockaddr_in* clientAddr, socklen_t clientAddrSize);

int m_set_sock_addr(struct sockaddr_in* sockAddr, char* address, uint16_t port);

int m_set_reuse_addr(int socket, int value);

int m_set_keep_alive(int socket, int value);

int m_set_tcp_no_delay(int socket, int value);

int m_get_err_opt(int socket, int* ptr);

int m_set_nonblocking(int socket);

int m_bind(int socket, struct sockaddr_in* sockAddr, socklen_t size);

int m_listen(int socket, int backlog);

int m_err_inprogress();

int m_connect(int socket, struct sockaddr_in* sockAddr, socklen_t size);

ssize_t m_send(int socket, void* buf, size_t len);

ssize_t m_recv(int socket, void* buf, size_t len);

int m_close(int fd);

int m_shutdown_write(int fd);

int m_errno();

#endif
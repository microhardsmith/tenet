#ifndef LIB_WIN_H
#define LIB_WIN_H

__declspec(dllexport) void* w_epoll_create();

__declspec(dllexport) int w_epoll_ctl_add(void* handle, SOCKET socket, struct epoll_event* event);

__declspec(dllexport) int w_epoll_ctl_del(void* handle, SOCKET socket);

__declspec(dllexport) int w_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout);

__declspec(dllexport) int w_epoll_close(void* handle);

__declspec(dllexport) int w_address_len();

__declspec(dllexport) int w_address(struct sockaddr_in* clientAddr, char* addrStr, int len);

__declspec(dllexport) int w_port(struct sockaddr_in* clientAddr);

__declspec(dllexport) SOCKET w_socket_create();

__declspec(dllexport) int w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize);

__declspec(dllexport) int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, int port);

__declspec(dllexport) int w_set_reuse_addr(SOCKET socket, boolean value);

__declspec(dllexport) int w_set_keep_alive(SOCKET socket, boolean value);

__declspec(dllexport) int w_set_tcp_no_delay(SOCKET socket, boolean value);

__declspec(dllexport) int w_set_nonblocking(SOCKET socket);

__declspec(dllexport) int w_bind(struct sockaddr_in* sockAddr, SOCKET socket, int size);

__declspec(dllexport) int w_listen(SOCKET socket, int backlog);

__declspec(dllexport) int w_recv(SOCKET socket, char* buf, int len);

__declspec(dllexport) int w_close_socket(SOCKET socket);

__declspec(dllexport) int wsa_get_last_error();

__declspec(dllexport) int wsa_clean_up();

#endif
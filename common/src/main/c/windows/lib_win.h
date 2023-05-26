#ifndef LIB_WIN_H
#define LIB_WIN_H

__declspec(dllexport) FILE* g_stdout();

__declspec(dllexport) FILE* g_stderr();

__declspec(dllexport) void g_print(char* str, FILE* stream);

__declspec(dllexport) int w_connect_block_code();

__declspec(dllexport) int w_send_block_code();

__declspec(dllexport) SOCKET w_invalid_socket();

__declspec(dllexport) int w_address_len();

__declspec(dllexport) void* w_epoll_create();

__declspec(dllexport) int w_epoll_ctl(void* handle, int op, SOCKET socket, struct epoll_event* event);

__declspec(dllexport) int w_epoll_wait(void* handle, struct epoll_event* events, int maxevents, int timeout);

__declspec(dllexport) int w_epoll_close(void* handle);

__declspec(dllexport) int w_address(struct sockaddr_in* clientAddr, char* addrStr, int len);

__declspec(dllexport) u_short w_port(struct sockaddr_in* clientAddr);

__declspec(dllexport) SOCKET w_socket_create();

__declspec(dllexport) SOCKET w_accept(SOCKET socket, struct sockaddr_in* clientAddr, int clientAddrSize);

__declspec(dllexport) int w_set_sock_addr(struct sockaddr_in* sockAddr, char* address, u_short port);

__declspec(dllexport) int w_set_reuse_addr(SOCKET socket, int value);

__declspec(dllexport) int w_set_keep_alive(SOCKET socket, int value);

__declspec(dllexport) int w_set_tcp_no_delay(SOCKET socket, int value);

__declspec(dllexport) int w_get_err_opt(SOCKET socket, int* ptr);

__declspec(dllexport) int w_set_nonblocking(SOCKET socket);

__declspec(dllexport) int w_bind(SOCKET socket, struct sockaddr_in* sockAddr, int size);

__declspec(dllexport) int w_connect(SOCKET socket, struct sockaddr_in* sockAddr, int size);

__declspec(dllexport) int w_listen(SOCKET socket, int backlog);

__declspec(dllexport) int w_recv(SOCKET socket, char* buf, int len);

__declspec(dllexport) int w_send(SOCKET socket, char* buf, int len);

__declspec(dllexport) int w_close_socket(SOCKET socket);

__declspec(dllexport) int w_shutdown_write(SOCKET socket);

__declspec(dllexport) int w_get_last_error();

__declspec(dllexport) int w_clean_up();

#endif
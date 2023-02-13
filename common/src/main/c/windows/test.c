#include <stdio.h>
#include "test.h"
// #include "wepoll.h"
// #include <WinSock2.h>

// #pragma comment (lib, "ws2_32.lib")

int hello(book* ptr) {
    printf("c address : %p \n", ptr);
    ptr->a = 4;
    ptr->b = 5;
    return 0;
}

int main() {
    book t;
    int i = hello(&t);
    printf("size : %llu \n", sizeof(book));
    printf("result : %d \n", i);
    printf("value a : %d \n", t.a);
    printf("value b : %d \n", t.b);
}
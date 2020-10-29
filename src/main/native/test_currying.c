#include <stdio.h>

typedef int (*two_var_func) (int, int);
typedef int (*one_var_func) (int);

int add_int (int a, int b) {
    return a+b;
}

one_var_func partial (two_var_func f, int a) {
    int g (int b) {
        return f (a, b);
    }
    return g;
}

int main (void) {
    int a = 1;
    int b = 2;
    printf ("%d\n", add_int (a, b));
    printf ("%d\n", partial (add_int, a) (b));
}

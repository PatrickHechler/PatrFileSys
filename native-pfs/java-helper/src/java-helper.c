/*
 * java-helper.c
 *
 *  Created on: Sep 24, 2022
 *      Author: pat
 */

extern void vfrom(void (**func)(void)) {
	(*func)();
}

extern int ifrom(int (**func)(void)) {
	return (*func)();
}

extern long lfrom(long (**func)(void)) {
	return (*func)();
}

extern void* pfrom(void* (**func)(void)) {
	return (*func)();
}

extern void vfromi(void (**func)(int), int a0) {
	(*func)(a0);
}

extern int ifromi(int (**func)(int), int a0) {
	return (*func)(a0);
}

extern long lfromi(long (**func)(int), int a0) {
	return (*func)(a0);
}

extern void* pfromi(void* (**func)(int), int a0) {
	return (*func)(a0);
}

extern void vfroml(void (**func)(long), long a0) {
	(*func)(a0);
}

extern int ifroml(int (**func)(long), long a0) {
	return (*func)(a0);
}

extern long lfroml(long (**func)(long), long a0) {
	return (*func)(a0);
}

extern void* pfroml(void* (**func)(long), long a0) {
	return (*func)(a0);
}

extern void vfromp(void (**func)(void*), void *a0) {
	(*func)(a0);
}

extern int ifromp(int (**func)(void*), void *a0) {
	return (*func)(a0);
}

extern long lfromp(long (**func)(void*), void *a0) {
	return (*func)(a0);
}

extern void* pfromp(void* (**func)(void*), void *a0) {
	return (*func)(a0);
}

extern void vfromii(void (**func)(int, int), int a0, int a1) {
	(*func)(a0, a1);
}

extern int ifromii(int (**func)(int, int), int a0, int a1) {
	return (*func)(a0, a1);
}

extern long lfromii(long (**func)(int, int), int a0, int a1) {
	return (*func)(a0, a1);
}

extern void* pfromii(void* (**func)(int, int), int a0, int a1) {
	return (*func)(a0, a1);
}

extern void vfromli(void (**func)(long, int), long a0, int a1) {
	(*func)(a0, a1);
}

extern int ifromli(int (**func)(long, int), long a0, int a1) {
	return (*func)(a0, a1);
}

extern long lfromli(long (**func)(long, int), long a0, int a1) {
	return (*func)(a0, a1);
}

extern void* pfromli(void* (**func)(long, int), long a0, int a1) {
	return (*func)(a0, a1);
}

extern void vfrompi(void (**func)(void*, int), void *a0, int a1) {
	(*func)(a0, a1);
}

extern int ifrompi(int (**func)(void*, int), void *a0, int a1) {
	return (*func)(a0, a1);
}

extern long lfrompi(long (**func)(void*, int), void *a0, int a1) {
	return (*func)(a0, a1);
}

extern void* pfrompi(void* (**func)(void*, int), void *a0, int a1) {
	return (*func)(a0, a1);
}

extern void vfromil(void (**func)(int, long), int a0, long a1) {
	(*func)(a0, a1);
}

extern int ifromil(int (**func)(int, long), int a0, long a1) {
	return (*func)(a0, a1);
}

extern long lfromil(long (**func)(int, long), int a0, long a1) {
	return (*func)(a0, a1);
}

extern void* pfromil(void* (**func)(int, long), int a0, long a1) {
	return (*func)(a0, a1);
}

extern void vfromll(void (**func)(long, long), long a0, long a1) {
	(*func)(a0, a1);
}

extern int ifromll(int (**func)(long, long), long a0, long a1) {
	return (*func)(a0, a1);
}

extern long lfromll(long (**func)(long, long), long a0, long a1) {
	return (*func)(a0, a1);
}

extern void* pfromll(void* (**func)(long, long), long a0, long a1) {
	return (*func)(a0, a1);
}

extern void vfrompl(void (**func)(void*, long), void *a0, long a1) {
	(*func)(a0, a1);
}

extern int ifrompl(int (**func)(void*, long), void *a0, long a1) {
	return (*func)(a0, a1);
}

extern long lfrompl(long (**func)(void*, long), void *a0, long a1) {
	return (*func)(a0, a1);
}

extern void* pfrompl(void* (**func)(void*, long), void *a0, long a1) {
	return (*func)(a0, a1);
}

extern void vfromip(void (**func)(int, void*), int a0, void *a1) {
	(*func)(a0, a1);
}

extern int ifromip(int (**func)(int, void*), int a0, void *a1) {
	return (*func)(a0, a1);
}

extern long lfromip(long (**func)(int, void*), int a0, void *a1) {
	return (*func)(a0, a1);
}

extern void* pfromip(void* (**func)(int, void*), int a0, void *a1) {
	return (*func)(a0, a1);
}

extern void vfromlp(void (**func)(long, void*), long a0, void *a1) {
	(*func)(a0, a1);
}

extern int ifromlp(int (**func)(long, void*), long a0, void *a1) {
	return (*func)(a0, a1);
}

extern long lfromlp(long (**func)(long, void*), long a0, void *a1) {
	return (*func)(a0, a1);
}

extern void* pfromlp(void* (**func)(long, void*), long a0, void *a1) {
	return (*func)(a0, a1);
}

extern void vfrompp(void (**func)(void*, void*), void *a0, void *a1) {
	(*func)(a0, a1);
}

extern int ifrompp(int (**func)(void*, void*), void *a0, void *a1) {
	return (*func)(a0, a1);
}

extern long lfrompp(long (**func)(void*, void*), void *a0, void *a1) {
	return (*func)(a0, a1);
}

extern void* pfrompp(void* (**func)(void*, void*), void *a0, void *a1) {
	return (*func)(a0, a1);
}




extern void vdfrom(void (*func)(void)) {
	func();
}

extern int idfrom(int (*func)(void)) {
	return func();
}

extern long ldfrom(long (*func)(void)) {
	return func();
}

extern void* pdfrom(void* (*func)(void)) {
	return func();
}

extern void vdfromi(void (*func)(int), int a0) {
	func(a0);
}

extern int idfromi(int (*func)(int), int a0) {
	return func(a0);
}

extern long ldfromi(long (*func)(int), int a0) {
	return func(a0);
}

extern void* pdfromi(void* (*func)(int), int a0) {
	return func(a0);
}

extern void vdfroml(void (*func)(long), long a0) {
	func(a0);
}

extern int idfroml(int (*func)(long), long a0) {
	return func(a0);
}

extern long ldfroml(long (*func)(long), long a0) {
	return func(a0);
}

extern void* pdfroml(void* (*func)(long), long a0) {
	return func(a0);
}

extern void vdfromp(void (*func)(void*), void *a0) {
	func(a0);
}

extern int idfromp(int (*func)(void*), void *a0) {
	return func(a0);
}

extern long ldfromp(long (*func)(void*), void *a0) {
	return func(a0);
}

extern void* pdfromp(void* (*func)(void*), void *a0) {
	return func(a0);
}

extern void vdfromii(void (*func)(int, int), int a0, int a1) {
	func(a0, a1);
}

extern int idfromii(int (*func)(int, int), int a0, int a1) {
	return func(a0, a1);
}

extern long ldfromii(long (*func)(int, int), int a0, int a1) {
	return func(a0, a1);
}

extern void* pdfromii(void* (*func)(int, int), int a0, int a1) {
	return func(a0, a1);
}

extern void vdfromli(void (*func)(long, int), long a0, int a1) {
	func(a0, a1);
}

extern int idfromli(int (*func)(long, int), long a0, int a1) {
	return func(a0, a1);
}

extern long ldfromli(long (*func)(long, int), long a0, int a1) {
	return func(a0, a1);
}

extern void* pdfromli(void* (*func)(long, int), long a0, int a1) {
	return func(a0, a1);
}

extern void vdfrompi(void (*func)(void*, int), void *a0, int a1) {
	func(a0, a1);
}

extern int idfrompi(int (*func)(void*, int), void *a0, int a1) {
	return func(a0, a1);
}

extern long ldfrompi(long (*func)(void*, int), void *a0, int a1) {
	return func(a0, a1);
}

extern void* pdfrompi(void* (*func)(void*, int), void *a0, int a1) {
	return func(a0, a1);
}

extern void vdfromil(void (*func)(int, long), int a0, long a1) {
	func(a0, a1);
}

extern int idfromil(int (*func)(int, long), int a0, long a1) {
	return func(a0, a1);
}

extern long ldfromil(long (*func)(int, long), int a0, long a1) {
	return func(a0, a1);
}

extern void* pdfromil(void* (*func)(int, long), int a0, long a1) {
	return func(a0, a1);
}

extern void vdfromll(void (*func)(long, long), long a0, long a1) {
	func(a0, a1);
}

extern int idfromll(int (*func)(long, long), long a0, long a1) {
	return func(a0, a1);
}

extern long ldfromll(long (*func)(long, long), long a0, long a1) {
	return func(a0, a1);
}

extern void* pdfromll(void* (*func)(long, long), long a0, long a1) {
	return func(a0, a1);
}

extern void vdfrompl(void (*func)(void*, long), void *a0, long a1) {
	func(a0, a1);
}

extern int idfrompl(int (*func)(void*, long), void *a0, long a1) {
	return func(a0, a1);
}

extern long ldfrompl(long (*func)(void*, long), void *a0, long a1) {
	return func(a0, a1);
}

extern void* pdfrompl(void* (*func)(void*, long), void *a0, long a1) {
	return func(a0, a1);
}

extern void vdfromip(void (*func)(int, void*), int a0, void *a1) {
	func(a0, a1);
}

extern int idfromip(int (*func)(int, void*), int a0, void *a1) {
	return func(a0, a1);
}

extern long ldfromip(long (*func)(int, void*), int a0, void *a1) {
	return func(a0, a1);
}

extern void* pdfromip(void* (*func)(int, void*), int a0, void *a1) {
	return func(a0, a1);
}

extern void vdfromlp(void (*func)(long, void*), long a0, void *a1) {
	func(a0, a1);
}

extern int idfromlp(int (*func)(long, void*), long a0, void *a1) {
	return func(a0, a1);
}

extern long ldfromlp(long (*func)(long, void*), long a0, void *a1) {
	return func(a0, a1);
}

extern void* pdfromlp(void* (*func)(long, void*), long a0, void *a1) {
	return func(a0, a1);
}

extern void vdfrompp(void (*func)(void*, void*), void *a0, void *a1) {
	func(a0, a1);
}

extern int idfrompp(int (*func)(void*, void*), void *a0, void *a1) {
	return func(a0, a1);
}

extern long ldfrompp(long (*func)(void*, void*), void *a0, void *a1) {
	return func(a0, a1);
}

extern void* pdfrompp(void* (*func)(void*, void*), void *a0, void *a1) {
	return func(a0, a1);
}

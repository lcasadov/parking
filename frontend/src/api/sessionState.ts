// Flag module-scope: ¿cree la SPA que hay una sesion activa?
//
// Decision de diseño (fix bug #9): el interceptor 401 solo debe tratar un 401
// como "sesion expirada" si en ese momento habia una sesion que la SPA creia
// activa. Un flag sincronizado por AuthProvider (fuera de React) permite al
// interceptor Axios consultarlo sin acoplarse al arbol de componentes ni
// introducir dependencias circulares (este modulo no importa nada).
let sessionActive = false;

export function setSessionActive(active: boolean): void {
  sessionActive = active;
}

export function isSessionActive(): boolean {
  return sessionActive;
}

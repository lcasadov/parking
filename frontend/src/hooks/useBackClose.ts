import { useEffect, useRef } from 'react';

// En móvil, el gesto/botón "atrás" del navegador debe CERRAR el modal abierto, no
// navegar a otra ruta ni salir de la pantalla. Al montar, empuja una entrada de
// historial "señuelo"; un `popstate` (atrás) invoca `onClose`. Al cerrarse por la
// UI (X, botón, Escape), consume esa entrada señuelo para no ensuciar el historial.
//
// Anidamiento: varios modales apilan cada uno su entrada, así que "atrás" cierra
// el de encima primero (LIFO), que es el comportamiento esperado.
export function useBackClose(onClose: () => void): void {
  const onCloseRef = useRef(onClose);
  onCloseRef.current = onClose;

  useEffect(() => {
    window.history.pushState({ modalOpen: true }, '');
    const handlePop = (): void => onCloseRef.current();
    window.addEventListener('popstate', handlePop);

    // Cleanup: SOLO quitamos el listener. NO llamamos a history.back() aquí: en
    // React StrictMode (dev) el efecto se monta→desmonta→monta, y un back() en el
    // desmontaje intermedio dispara un popstate que cerraría el modal recién
    // remontado (bug "no abre / se cierra al instante"). El coste de no revertir
    // la entrada señuelo es dejar una entra de historial de más al cerrar por UI
    // (un "atrás" extra inofensivo), muy preferible a romper la apertura del modal.
    return () => {
      window.removeEventListener('popstate', handlePop);
    };
  }, []);
}

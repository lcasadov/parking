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

    return () => {
      window.removeEventListener('popstate', handlePop);
      // Si la entrada señuelo sigue presente, el cierre vino de la UI (no del
      // "atrás"): la retiramos para que el siguiente "atrás" del usuario funcione
      // con normalidad. Si vino del "atrás", el popstate ya la consumió.
      if (window.history.state?.modalOpen) {
        window.history.back();
      }
    };
  }, []);
}

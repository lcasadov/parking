import * as RadixTooltip from '@radix-ui/react-tooltip';
import { type ReactNode } from 'react';

interface TooltipProps {
  content: ReactNode;
  children: ReactNode;
  side?: 'top' | 'right' | 'bottom' | 'left';
}

// Proveedor de tooltips: montar UNA vez cerca de la raíz de la app para compartir
// el retardo entre tooltips (comportamiento nativo de Radix). Opcional pero
// recomendado; TooltipRoot funciona igual sin él.
export function TooltipProvider({ children }: { children: ReactNode }) {
  return (
    <RadixTooltip.Provider delayDuration={200} skipDelayDuration={300}>
      {children}
    </RadixTooltip.Provider>
  );
}

// Tooltip accesible sobre Radix Tooltip (Ola A · primitivas). Aparece en hover y
// foco de teclado; se oculta con Escape. Escala desde el disparador (origen
// provisto por Radix). Solo para contenido no esencial (apple-design: la utilidad
// manda) — nunca la única vía de una acción. Reduced-motion se respeta en CSS.
export function Tooltip({ content, children, side = 'top' }: TooltipProps) {
  return (
    <RadixTooltip.Root>
      <RadixTooltip.Trigger asChild>{children}</RadixTooltip.Trigger>
      <RadixTooltip.Portal>
        <RadixTooltip.Content className="rx-tooltip" side={side} sideOffset={6}>
          {content}
          <RadixTooltip.Arrow className="rx-tooltip-arrow" />
        </RadixTooltip.Content>
      </RadixTooltip.Portal>
    </RadixTooltip.Root>
  );
}

# Tasks — redesign-design-system

## 1. Tokens y fuentes
- [ ] 1.1 Definir variables CSS de color (paleta ALEATICA + neutros) en un archivo de tema global
- [ ] 1.2 Cargar Cormorant Garamond + Mulish (link Google Fonts o self-host en /public/fonts)
- [ ] 1.3 Definir escala tipográfica (h1 52, h2 34, numerales 34–42, cuerpo 16, labels 11 uppercase)

## 2. Estilos base
- [ ] 2.1 Botón primario (green/#fff/800), secundario (panel+line), pill, avatar
- [ ] 2.2 Estilos de panel (radio 12–14px, sombra 0 30px 60px -48px rgba(43,42,40,.5))
- [ ] 2.3 Mapa de estado→color como tokens reutilizables (ocupado/liberado/pendiente/solicitud/libre)

## 3. Shell
- [ ] 3.1 Sidebar 252px con logo ALEATICA, secciones Gestión/Operativa e item activo (border-left 3px accent)
- [ ] 3.2 Header de página (eyebrow + título serif + descripción)
- [ ] 3.3 Tarjeta de usuario al pie del sidebar
- [ ] 3.4 Contenedor de layout que envuelve las rutas de la app

## 4. Verificación
- [ ] 4.1 Confirmar que no se han tocado llamadas a API ni lógica
- [ ] 4.2 Revisar responsive del shell
- [ ] 4.3 Marcar todas las tasks y solicitar diff por archivo antes de aplicar

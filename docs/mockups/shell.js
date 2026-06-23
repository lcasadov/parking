// Renderiza top-nav, app-header y sidebar comunes
function renderShell(opts) {
  const pages = [
    {file:'01-calendario-semanal.html',label:'1. Calendario'},
    {file:'02-solicitudes-pendientes.html',label:'2. Solicitudes'},
    {file:'03-empleados-asignacion-fija.html',label:'3. Empleados'},
    {file:'04-modal-edicion-empleado.html',label:'4. Modal empleado'},
    {file:'05-modal-aprobar-solicitud.html',label:'5. Modal aprobar'},
    {file:'06-modal-rechazar-solicitud.html',label:'6. Modal rechazar'},
    {file:'07-empleado-movil.html',label:'7. Móvil'},
    {file:'08-plazas.html',label:'8. Plazas'},
    {file:'09-visitantes.html',label:'9. Visitantes'},
    {file:'10-modal-reserva-visita.html',label:'10. Reserva visita'},
    {file:'11-auditoria.html',label:'11. Auditoría'},
    {file:'12-login.html',label:'12. Login'},
    {file:'13-cambiar-password.html',label:'13. Cambiar contraseña'},
    {file:'14-liberar-plaza-movil.html',label:'14. Liberar (móvil)'},
    {file:'15-modal-reset-password.html',label:'15. Reset contraseña'},
    {file:'16-modal-plaza.html',label:'16. Plaza (form)'},
    {file:'17-preferencias.html',label:'17. Preferencias'},
    {file:'18-modo-oscuro.html',label:'18. Modo oscuro'},
    {file:'19-modal-sesion-expirada.html',label:'19. Sesión expirada'},
  ];
  const topnav = `<div class="topnav">
    <span class="label">parking · Mockups</span>
    ${pages.map(p => `<a href="${p.file}" class="${opts.active === p.file ? 'active' : ''}">${p.label}</a>`).join('')}
    <a href="index.html" style="margin-left:auto"><i class="ti ti-home"></i> Inicio</a>
  </div>`;

  const sidebarItems = [
    {icon:'ti-home',label:'Inicio'},
    {icon:'ti-calendar-event',label:'Asignación semanal',key:'calendar'},
    {icon:'ti-inbox',label:'Solicitudes',badge:5,key:'requests'},
    {icon:'ti-users',label:'Empleados',key:'employees'},
    {icon:'ti-parking',label:'Plazas',key:'spaces'},
    {icon:'ti-armchair',label:'Puestos',key:'desks'},
    {icon:'ti-map-2',label:'Plano',key:'plan'},
    {icon:'ti-user-plus',label:'Visitantes',key:'visitors'},
    {icon:'ti-history',label:'Auditoría',key:'audit'},
    {icon:'ti-settings',label:'Administración',chevron:true},
  ];

  const sidebar = `<div class="sidebar">
    ${sidebarItems.map(i => `
      <div class="nav-item ${opts.activeSidebar === i.key ? 'active' : ''}">
        <i class="ti ${i.icon}" style="font-size:18px"></i>${i.label}
        ${i.badge ? `<span class="badge-red">${i.badge}</span>` : ''}
        ${i.chevron ? `<i class="ti ti-chevron-right" style="margin-left:auto;font-size:14px"></i>` : ''}
      </div>
    `).join('')}
  </div>`;

  const header = `<div class="app-header">
    <svg class="curve" viewBox="0 0 340 64" preserveAspectRatio="none">
      <path d="M0,40 Q120,5 220,30 T340,20 L340,0 L0,0 Z" fill="#c0dd97" opacity="0.85"/>
      <path d="M120,55 Q200,25 280,45 T340,40 L340,55 L120,64 Z" fill="#ef9f27" opacity="0.9"/>
    </svg>
    <div class="brand-block">
      <div class="logo-dot"></div>
      <div>
        <div class="brand-name">parking</div>
        <div class="brand-sub">ALEATICA</div>
      </div>
      <div class="page-title">${opts.title}</div>
    </div>
    <div class="avatar">RB</div>
  </div>`;

  document.getElementById('topnav-slot').innerHTML = topnav;
  document.getElementById('header-slot').innerHTML = header;
  if (opts.showSidebar !== false) {
    document.getElementById('sidebar-slot').innerHTML = sidebar;
  }
}

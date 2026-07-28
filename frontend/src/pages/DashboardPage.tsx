import { useMemo, useState } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ApproveRequestModal } from '../components/ApproveRequestModal';
import { Avatar } from '../components/Avatar';
import { Button } from '../components/Button';
import { Spinner } from '../components/Spinner';
import { useAuth } from '../auth/useAuth';
import { useAuditQuery } from '../hooks/useAudit';
import { useAvailabilityQuery } from '../hooks/useCalendar';
import { useEmployeesQuery } from '../hooks/useEmployees';
import { useOccupancyQuery } from '../hooks/useOccupancy';
import { usePendingRequestsQuery } from '../hooks/useRequests';
import { ROUTES } from '../routes/paths';
import { formatDateTime } from '../utils/audit';
import { initialsOf } from '../utils/initials';
import { todayIso } from '../utils/requests';
import type { AuditLogEntry } from '../types/audit';
import type { Employee } from '../types/employee';
import type { Request, ResourceType } from '../types/request';

const LOOKUP_SIZE = 100;
const PENDING_PREVIEW = 4;
const ACTIVITY_PREVIEW = 5;
const MORNING_END = 12;
const EVENING_START = 20;

// Clave de saludo segun la hora local (mañana / tarde / noche).
function greetingKey(now: Date): 'morning' | 'afternoon' | 'evening' {
  const hour = now.getHours();
  if (hour < MORNING_END) {
    return 'morning';
  }
  if (hour < EVENING_START) {
    return 'afternoon';
  }
  return 'evening';
}

// Fecha larga localizada ("martes, 24 de julio"), usando el idioma activo de i18n.
function formatToday(language: string, now: Date): string {
  return new Intl.DateTimeFormat(language, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(now);
}

// Nombre humano legible de una accion de auditoria (enum tecnico -> texto) como
// FALLBACK cuando no hay traduccion especifica.
function humanizeAction(action: string): string {
  const spaced = action.replace(/_/g, ' ').toLowerCase();
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

// Etiqueta traducida de la accion de auditoria; si el codigo no esta mapeado,
// cae al texto humanizado (nunca muestra el enum crudo salvo desconocido).
function actionLabel(action: string, t: TFunction): string {
  return t(`dashboard.activity.action.${action}`, { defaultValue: humanizeAction(action) });
}

// Etiqueta traducida del tipo de entidad (chip); clave insensible a mayus/minus.
function entityLabel(entityType: string, t: TFunction): string {
  const key = entityType.replace(/[^A-Za-z]/g, '').toUpperCase();
  return t(`dashboard.activity.entity.${key}`, { defaultValue: entityType });
}

function buildEmployeeMap(employees: Employee[]): Map<number, Employee> {
  const map = new Map<number, Employee>();
  for (const employee of employees) {
    map.set(employee.id, employee);
  }
  return map;
}

// Sparkline decorativa (mockup): trazo + area suave. El path es ilustrativo.
function Sparkline({ stroke, fill }: { stroke: string; fill?: string }) {
  return (
    <svg className="dash-kpi-spark" viewBox="0 0 96 44" preserveAspectRatio="none" aria-hidden="true">
      <path
        d="M0 34 12 30 24 32 36 22 48 24 60 14 72 18 84 9 96 12"
        stroke={stroke}
        fill="none"
        strokeWidth="2"
      />
      {fill ? <path d="M0 34 12 30 24 32 36 22 48 24 60 14 72 18 84 9 96 12V44H0Z" fill={fill} /> : null}
    </svg>
  );
}

interface KpiCardProps {
  dot: string;
  label: string;
  value: string;
  unit?: string;
  sub: string;
  spark?: { stroke: string; fill?: string };
}

function KpiCard({ dot, label, value, unit, sub, spark }: KpiCardProps) {
  return (
    <div className="card dash-kpi">
      <div className="dash-kpi-lbl">
        <span className="dash-kpi-dot" style={{ background: dot }} />
        {label}
      </div>
      <div className="dash-kpi-val">
        {value}
        {unit ? <small>{unit}</small> : null}
      </div>
      <div className="dash-kpi-sub">{sub}</div>
      {spark ? <Sparkline stroke={spark.stroke} fill={spark.fill} /> : null}
    </div>
  );
}

interface PendingRowProps {
  request: Request;
  employee?: Employee;
  onApprove: (id: number) => void;
}

function PendingRow({ request, employee, onApprove }: PendingRowProps) {
  const { t } = useTranslation();
  const name = employee
    ? `${employee.firstName} ${employee.lastName}`
    : `#${request.employeeId}`;
  const initials = employee ? initialsOf(employee) : name.slice(0, 2).toUpperCase();
  const resourceType: ResourceType = request.resourceType ?? 'PARKING';
  const resourceLine = t('dashboard.pending.resourceLine', {
    resource: t(`dashboard.resourceType.${resourceType}`),
    date: request.requestedDate,
  });
  return (
    <div className="dash-row">
      <div className="dash-row-who">
        <Avatar size="sm" initials={initials} label={name} seed={name} />
        <div>
          <b>{name}</b>
          <small>{resourceLine}</small>
        </div>
      </div>
      <div className="dash-row-rt">
        <span className="pill pend">{t('dashboard.pending.pill')}</span>
        <Button variant="green" icon="check" onClick={() => onApprove(request.id)}>
          {t('dashboard.pending.approve')}
        </Button>
      </div>
    </div>
  );
}

interface PendingPanelProps {
  isLoading: boolean;
  requests: Request[];
  employeeMap: Map<number, Employee>;
  onApprove: (id: number) => void;
}

function PendingPanelBody({ isLoading, requests, employeeMap, onApprove }: PendingPanelProps) {
  const { t } = useTranslation();
  if (isLoading) {
    return (
      <div className="dash-empty">
        <Spinner />
      </div>
    );
  }
  if (requests.length === 0) {
    return <p className="dash-empty">{t('dashboard.pending.empty')}</p>;
  }
  return (
    <>
      {requests.map((request) => (
        <PendingRow
          key={request.id}
          request={request}
          employee={employeeMap.get(request.employeeId)}
          onApprove={onApprove}
        />
      ))}
    </>
  );
}

function ActivityRow({ entry }: { entry: AuditLogEntry }) {
  const { t } = useTranslation();
  const actor = entry.actorEmployeeId
    ? `#${entry.actorEmployeeId}`
    : t('dashboard.activity.system');
  return (
    <div className="dash-row">
      <div className="dash-row-who">
        <div>
          <b>{actionLabel(entry.action, t)}</b>
          <small>
            {t('dashboard.activity.actor', { actor })} · {formatDateTime(entry.occurredAt)}
          </small>
        </div>
      </div>
      <span className="dash-chip">{entityLabel(entry.entityType, t)}</span>
    </div>
  );
}

interface ActivityPanelProps {
  isLoading: boolean;
  entries: AuditLogEntry[];
}

function ActivityPanelBody({ isLoading, entries }: ActivityPanelProps) {
  const { t } = useTranslation();
  if (isLoading) {
    return (
      <div className="dash-empty">
        <Spinner />
      </div>
    );
  }
  if (entries.length === 0) {
    return <p className="dash-empty">{t('dashboard.activity.empty')}</p>;
  }
  return (
    <>
      {entries.map((entry) => (
        <ActivityRow key={entry.id} entry={entry} />
      ))}
    </>
  );
}

// Panel de INICIO del admin (rediseño 2026, "Panel" del mockup): saludo + fecha,
// fila de KPIs (ocupación de hoy, solicitudes pendientes, plazas/puestos libres)
// con sparklines, panel de solicitudes pendientes accionables (aprobar in situ) y
// panel de actividad reciente. Se alimenta de endpoints existentes: /occupancy,
// /availability (por tipo), /requests (pendientes), /employees, /audit.
export function DashboardPage() {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const now = useMemo(() => new Date(), []);
  const today = todayIso(now);
  const [approveId, setApproveId] = useState<number | null>(null);

  const occupancyQuery = useOccupancyQuery(today);
  const freeParkingQuery = useAvailabilityQuery(today, 'PARKING');
  const freeDeskQuery = useAvailabilityQuery(today, 'DESK');
  const pendingQuery = usePendingRequestsQuery({ page: 0, size: PENDING_PREVIEW });
  const employeesQuery = useEmployeesQuery({ page: 0, size: LOOKUP_SIZE });
  const auditQuery = useAuditQuery({ page: 0, size: ACTIVITY_PREVIEW });

  const employees = useMemo(() => employeesQuery.data?.content ?? [], [employeesQuery.data]);
  const employeeMap = useMemo(() => buildEmployeeMap(employees), [employees]);

  const occupied = useMemo(
    () => occupancyQuery.data?.occupiedResources ?? [],
    [occupancyQuery.data],
  );
  const occupiedParking = occupied.filter((item) => item.resourceType === 'PARKING').length;
  const occupiedDesk = occupied.filter((item) => item.resourceType === 'DESK').length;
  const freeParking = freeParkingQuery.data?.availableResources.length ?? 0;
  const freeDesk = freeDeskQuery.data?.availableResources.length ?? 0;

  const totalParking = occupiedParking + freeParking;
  const totalDesk = occupiedDesk + freeDesk;
  const totalResources = totalParking + totalDesk;
  const occupiedTotal = occupied.length;
  const occupancyPct =
    totalResources > 0 ? Math.round((occupiedTotal / totalResources) * 100) : 0;

  const pendingList = pendingQuery.data?.content ?? [];
  const pendingCount = pendingQuery.data?.totalElements ?? 0;
  const newToday = pendingList.filter((request) => request.createdAt?.startsWith(today)).length;

  const activity = auditQuery.data?.content ?? [];

  const firstName = user?.firstName ?? '';
  const greeting = t(`dashboard.greeting.${greetingKey(now)}`);
  const hi = firstName ? `${greeting}, ${firstName}` : greeting;

  const approveTarget = pendingList.find((request) => request.id === approveId) ?? null;

  return (
    <section className="dashboard" aria-labelledby="dashboard-title">
      <header className="dash-head">
        <div>
          <p className="dash-hi">{hi}</p>
          <h1 id="dashboard-title" className="dash-title">
            {formatToday(i18n.language, now)}
          </h1>
        </div>
        <Link to={ROUTES.adminOccupancy} className="btn btn-white">
          {t('dashboard.seeWeek')}
        </Link>
      </header>

      <div className="dash-kpis">
        <KpiCard
          dot="var(--accent)"
          label={t('dashboard.kpi.occupancy')}
          value={String(occupancyPct)}
          unit="%"
          sub={t('dashboard.kpi.occupancySub', { occupied: occupiedTotal, total: totalResources })}
          spark={{ stroke: 'var(--accent)', fill: 'var(--accent-soft)' }}
        />
        <KpiCard
          dot="var(--pend)"
          label={t('dashboard.kpi.pending')}
          value={String(pendingCount)}
          sub={t('dashboard.kpi.pendingSub', { count: newToday })}
          spark={{ stroke: 'var(--pend)' }}
        />
        <KpiCard
          dot="var(--ok)"
          label={t('dashboard.kpi.parkingFree')}
          value={String(freeParking)}
          sub={t('dashboard.kpi.parkingFreeSub', { total: totalParking })}
        />
        <KpiCard
          dot="var(--info)"
          label={t('dashboard.kpi.deskFree')}
          value={String(freeDesk)}
          sub={t('dashboard.kpi.deskFreeSub', { total: totalDesk })}
        />
      </div>

      <div className="dash-grid">
        <div className="card">
          <div className="dash-panel-ttl">
            <h3>{t('dashboard.pending.title')}</h3>
            <Link to={ROUTES.adminRequests} className="dash-panel-link">
              {t('dashboard.pending.seeAll')}
            </Link>
          </div>
          <PendingPanelBody
            isLoading={pendingQuery.isLoading}
            requests={pendingList}
            employeeMap={employeeMap}
            onApprove={setApproveId}
          />
        </div>

        <div className="card">
          <div className="dash-panel-ttl">
            <h3>{t('dashboard.activity.title')}</h3>
          </div>
          <ActivityPanelBody isLoading={auditQuery.isLoading} entries={activity} />
        </div>
      </div>

      {approveTarget !== null ? (
        <ApproveRequestModal
          request={approveTarget}
          employee={employeeMap.get(approveTarget.employeeId)}
          onClose={() => setApproveId(null)}
          onApproved={() => setApproveId(null)}
        />
      ) : null}
    </section>
  );
}

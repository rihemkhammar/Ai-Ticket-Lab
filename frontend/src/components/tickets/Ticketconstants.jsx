import { TbTicket, TbClockHour4, TbCircleCheck } from 'react-icons/tb';

export const STATUS_CONFIG = {
  OPEN:        { label: 'Open',        dot: '#60a5fa' },
  IN_PROGRESS: { label: 'In Progress', dot: '#fb923c' },
  CLOSED:      { label: 'Closed',      dot: '#4ade80' },
};
export  const STAT_CONFIG = [
  { key: 'open',       label: 'Open',        Icon: TbTicket,      color: '#60a5fa', bg: 'rgba(96,165,250,0.1)',  border: 'rgba(96,165,250,0.2)'  },
  { key: 'inProgress', label: 'In Progress', Icon: TbClockHour4,  color: '#fb923c', bg: 'rgba(251,146,60,0.1)',  border: 'rgba(251,146,60,0.2)'  },
  { key: 'closed',     label: 'Closed',      Icon: TbCircleCheck, color: '#4ade80', bg: 'rgba(74,222,128,0.1)',  border: 'rgba(74,222,128,0.2)'  },
];

export const FILTER_MAP = {
  'All':         null,
  'Open':        'OPEN',
  'In Progress': 'IN_PROGRESS',
  'Closed':      'CLOSED',
};
 
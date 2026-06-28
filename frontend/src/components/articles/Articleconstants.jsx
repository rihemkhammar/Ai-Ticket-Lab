import { TbFileText, TbCalendarStats, TbShieldCheck } from 'react-icons/tb';

export const CATEGORY_CONFIG = {
  CONVEYOR: { label: 'Conveyor', dot: '#60a5fa', color: '#60a5fa', bg: 'rgba(96,165,250,0.12)',  text: '#60a5fa'  },
  MOTOR:    { label: 'Motor',    dot: '#fb923c', color: '#fb923c', bg: 'rgba(251,146,60,0.12)',   text: '#fb923c'  },
  PUMP:     { label: 'Pump',     dot: '#38bdf8', color: '#38bdf8', bg: 'rgba(56,189,248,0.12)',   text: '#38bdf8'  },
  SENSOR:   { label: 'Sensor',   dot: '#a78bfa', color: '#a78bfa', bg: 'rgba(167,139,250,0.12)', text: '#a78bfa'  },
  SAFETY:   { label: 'Safety',   dot: '#f87171', color: '#f87171', bg: 'rgba(248,113,113,0.12)', text: '#f87171'  },
};

export const ARTICLE_STAT_CONFIG = [
  { key: 'total',   label: 'Articles',         Icon: TbFileText,     color: '#60a5fa', bg: 'rgba(96,165,250,0.1)',  border: 'rgba(96,165,250,0.2)'  },
  { key: 'recent',  label: 'Added This Week',  Icon: TbCalendarStats, color: '#a78bfa', bg: 'rgba(167,139,250,0.1)', border: 'rgba(167,139,250,0.2)' },
  { key: 'safety',  label: 'Safety Articles',  Icon: TbShieldCheck,  color: '#f87171', bg: 'rgba(248,113,113,0.1)', border: 'rgba(248,113,113,0.2)' },
];


export const FILTER_MAP = {
  'All':      null,
  'Conveyor': 'CONVEYOR',
  'Motor':    'MOTOR',
  'Pump':     'PUMP',
  'Sensor':   'SENSOR',
  'Safety':   'SAFETY',
};
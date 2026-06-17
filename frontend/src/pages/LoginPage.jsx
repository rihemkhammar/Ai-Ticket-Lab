import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaEye, FaEyeSlash, FaSpinner } from "react-icons/fa";
import { TbRobot } from "react-icons/tb";
import { MdWifiOff, MdOutlineError } from "react-icons/md";
import { RiLockPasswordLine } from "react-icons/ri";
import { login as apiLogin } from '../services/api';

function Login() {
  const [username, setUsername]         = useState('');
  const [password, setPassword]         = useState('');
  const [error, setError]               = useState('');
  const [loading, setLoading]           = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [roleOverride, setRoleOverride] = useState('');

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await apiLogin({ username, password });
      console.log("[Login] data reçu après login :", data);

      // Priorité au rôle renvoyé par le backend, sinon override manuel
      const role = data.role || roleOverride;
      console.log("[Login] Rôle utilisé :", role);

      if (!role) {
        setError('no_role');
        return;
      }

      localStorage.setItem('role', role);

      if (role === 'ADMIN')           navigate('/admin/dashboard',      { replace: true });
      else if (role === 'TECHNICIAN') navigate('/technician/dashboard', { replace: true });
      else                            setError('unknown_role');

    } catch (err) {
      const status = err.response?.status;
      const msg    = err.response?.data?.message;
      console.error("[Login] Erreur :", status, msg, err);

      if (status === 401 || status === 403) setError('invalid_credentials');
      else if (!err.response)               setError('Failed to fetch');
      else                                  setError(msg || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  const errorTypes = {
    invalid_credentials: {
      title: 'Access Denied',
      desc:  'The email or password you entered is incorrect. Please try again.',
      Icon:  RiLockPasswordLine,
    },
    'Failed to fetch': {
      title: 'Server Unreachable',
      desc:  'Cannot connect to the server. Make sure the backend is running.',
      Icon:  MdWifiOff,
    },
    no_role: {
      title: 'Rôle manquant',
      desc:  'Le backend ne renvoie pas de rôle. Sélectionne un rôle manuellement ci-dessous.',
      Icon:  MdOutlineError,
    },
    unknown_role: {
      title: 'Rôle inconnu',
      desc:  'Le rôle reçu ne correspond à aucune page connue (ADMIN ou TECHNICIAN).',
      Icon:  MdOutlineError,
    },
  };

  const errInfo = errorTypes[error] || (error ? { title: 'Something went wrong', desc: error, Icon: MdOutlineError } : null);

  return (
    <div style={{
      width: '100vw', height: '100vh',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-main)',
      fontFamily: 'sans-serif',
      position: 'relative',
    }}>

      {/* Sign Up — top right */}
      <button
        onClick={() => navigate('/signup')}
        style={{
          position: 'absolute', top: '20px', right: '24px',
          padding: '11px 26px',
          background: 'var(--btn-gradient)',
          border: 'none', borderRadius: '10px',
          color: '#fff', fontSize: '14px', fontWeight: 600,
          cursor: 'pointer', letterSpacing: '0.3px',
        }}
      >
        Sign Up
      </button>

      {/* Card */}
      <div style={{
        width: '100%', maxWidth: '580px',
        background: 'var(--bg-card)',
        borderRadius: '28px',
        border: '1px solid rgba(77,124,199,0.2)',
        padding: '72px 72px',
        boxSizing: 'border-box',
        margin: '16px',
        boxShadow: '0 8px 48px rgba(6,11,25,0.6)',
      }}>

        {/* Logo */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '18px', marginBottom: '12px' }}>
          <div style={{
            width: '84px', height: '84px', borderRadius: '20px',
            background: 'var(--btn-gradient)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            <TbRobot style={{ width: '46px', height: '46px', color: '#fff' }} />
          </div>
          <div style={{ width: '1px', height: '58px', background: 'rgba(77,124,199,0.25)' }} />
          <div style={{ lineHeight: 1.15 }}>
            <div style={{ fontSize: '36px', fontWeight: 700, color: 'var(--text-main)', letterSpacing: '-0.3px' }}>
              AI Ticket{' '}
              <span style={{
                background: 'var(--btn-gradient)',
                WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', backgroundClip: 'text',
              }}>Lab</span>
            </div>
            <div style={{ fontSize: '13px', fontWeight: 500, letterSpacing: '3px', color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: '6px' }}>
              Smart support platform
            </div>
          </div>
        </div>

        <p style={{ textAlign: 'center', fontSize: '16px', color: 'var(--text-muted)', margin: '0 0 48px' }}>
          Access your workspace
        </p>

        {/* Error */}
        {errInfo && (
          <div style={{
            borderRadius: '12px',
            border: '1px solid rgba(220,38,38,0.3)',
            background: 'rgba(220,38,38,0.08)',
            padding: '18px 20px',
            marginBottom: '28px',
            display: 'flex', gap: '14px', alignItems: 'flex-start',
          }}>
            <errInfo.Icon style={{ width: '24px', height: '24px', color: '#f87171', flexShrink: 0, marginTop: '2px' }} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '15px', fontWeight: 600, color: '#f87171', marginBottom: '4px' }}>
                {errInfo.title}
              </div>
              <div style={{ fontSize: '13px', color: '#fca5a5', lineHeight: 1.5 }}>
                {errInfo.desc}
              </div>
            </div>
            <button
              onClick={() => setError('')}
              style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontSize: '22px', lineHeight: 1, flexShrink: 0, padding: '0 0 0 4px' }}
            >
              ×
            </button>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>

          {/* Username */}
          <div>
            <label style={{ display: 'block', fontSize: '15px', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '12px', letterSpacing: '0.3px' }}>
              Username
            </label>
            <input
              type="text" value={username} onChange={e => setUsername(e.target.value)}
              required placeholder="ex: UserName"
              style={{
                width: '100%', padding: '18px 22px',
                background: 'var(--bg-input)',
                border: '1px solid rgba(77,124,199,0.25)',
                borderRadius: '12px', color: 'var(--text-main)',
                fontSize: '16px', outline: 'none', boxSizing: 'border-box',
              }}
            />
          </div>

          {/* Password */}
          <div>
            <label style={{ display: 'block', fontSize: '15px', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '12px', letterSpacing: '0.3px' }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password} onChange={e => setPassword(e.target.value)}
                required placeholder="••••••••"
                style={{
                  width: '100%', padding: '18px 56px 18px 22px',
                  background: 'var(--bg-input)',
                  border: '1px solid rgba(77,124,199,0.25)',
                  borderRadius: '12px', color: 'var(--text-main)',
                  fontSize: '16px', outline: 'none', boxSizing: 'border-box',
                }}
              />
              <button
                type="button" tabIndex={-1}
                onClick={() => setShowPassword(!showPassword)}
                style={{
                  position: 'absolute', right: '14px', top: '50%', transform: 'translateY(-50%)',
                  background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '4px',
                }}
              >
                {showPassword ? <FaEyeSlash size={18} /> : <FaEye size={18} />}
              </button>
            </div>
          </div>

          {/* ─── Role Override (dev) — visible seulement si le backend ne renvoie pas de rôle ─── */}
          <div>
            <label style={{ display: 'block', fontSize: '15px', fontWeight: 500, color: 'var(--text-muted)', marginBottom: '12px', letterSpacing: '0.3px' }}>
              Rôle <span style={{ fontSize: '12px', color: 'rgba(148,163,184,0.6)', fontWeight: 400 }}>(override dev — à retirer en prod)</span>
            </label>
            <div style={{ display: 'flex', gap: '12px' }}>
              {['ADMIN', 'TECHNICIAN'].map(r => (
                <button
                  key={r} type="button"
                  onClick={() => setRoleOverride(prev => prev === r ? '' : r)}
                  style={{
                    flex: 1, padding: '14px',
                    background: roleOverride === r ? 'var(--btn-gradient)' : 'var(--bg-input)',
                    border: `1px solid ${roleOverride === r ? 'transparent' : 'rgba(77,124,199,0.25)'}`,
                    borderRadius: '12px',
                    color: roleOverride === r ? '#fff' : 'var(--text-muted)',
                    fontSize: '14px', fontWeight: 600, cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  {r}
                </button>
              ))}
            </div>
          </div>

          {/* Sign In */}
          <button
            type="submit" disabled={loading}
            style={{
              width: '100%', padding: '19px',
              background: 'var(--btn-gradient)',
              border: 'none', borderRadius: '12px',
              color: '#fff', fontSize: '17px', fontWeight: 600,
              cursor: loading ? 'not-allowed' : 'pointer',
              opacity: loading ? 0.7 : 1,
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
              marginTop: '8px', letterSpacing: '0.3px',
            }}
          >
            {loading
              ? <><FaSpinner size={19} style={{ animation: 'spin 1s linear infinite' }} /> Authenticating...</>
              : 'Sign In'}
          </button>

        </form>
      </div>

      <style>{`
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body, #root { width: 100%; height: 100%; }
        input::placeholder { color: var(--text-muted); }
        input:focus { border-color: var(--accent-blue) !important; box-shadow: 0 0 0 3px rgba(77,124,199,0.15); }
        @keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}

export default Login;
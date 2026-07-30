import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FaEye, FaEyeSlash, FaSpinner } from 'react-icons/fa';
import { TbRobot } from 'react-icons/tb';
import { MdWifiOff, MdOutlineError, MdCheckCircleOutline } from 'react-icons/md';
import { register as apiRegister } from '../services/api';

function Signup() {
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword]             = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [role, setRole]                             = useState('TECHNICIAN');
  const [error, setError]                           = useState('');
  const [success, setSuccess]                       = useState(false);
  const [loading, setLoading]                       = useState(false);

  const navigate = useNavigate();

  const set = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (form.password !== form.confirmPassword) {
      setError('password_mismatch');
      return;
    }
    if (form.password.length < 6) {
      setError('password_too_short');
      return;
    }

    setLoading(true);
    try {
      await apiRegister({ username: form.username, email: form.email, password: form.password, role });
      setSuccess(true);
      setTimeout(() => navigate('/login', { replace: true }), 2200);
    } catch (err) {
      const status = err.response?.status;
      const msg    = err.response?.data?.message;
      console.error('[Signup] Erreur :', status, msg, err);

      if (status === 409)   setError('user_exists');
      else if (!err.response) setError('Failed to fetch');
      else                    setError(msg || 'Something went wrong');
    } finally {
      setLoading(false);
    }
  };

  /* ─── Error  ─── */
  const errorTypes = {
    password_mismatch: {
      title: 'Passwords do not match',
      desc:  'Make sure both password fields are identical.',
      Icon:  MdOutlineError,
    },
    password_too_short: {
      title: 'Password too short',
      desc:  'Your password must be at least 6 characters long.',
      Icon:  MdOutlineError,
    },
    user_exists: {
      title: 'Account already exists',
      desc:  'An account with this username or email is already registered.',
      Icon:  MdOutlineError,
    },
    'Failed to fetch': {
      title: 'Server Unreachable',
      desc:  'Cannot connect to the server. Make sure the backend is running.',
      Icon:  MdWifiOff,
    },
  };

  const errInfo = errorTypes[error] || (error ? { title: 'Something went wrong', desc: error, Icon: MdOutlineError } : null);

  /* ─── Input style helper ─── */
  const inputStyle = {
    width: '100%', padding: '18px 22px',
    background: 'var(--bg-input)',
    border: '1px solid rgba(77,124,199,0.25)',
    borderRadius: '12px', color: 'var(--text-main)',
    fontSize: '16px', outline: 'none', boxSizing: 'border-box',
  };

  const labelStyle = {
    display: 'block', fontSize: '15px', fontWeight: 500,
    color: 'var(--text-muted)', marginBottom: '12px', letterSpacing: '0.3px',
  };

  return (
    <div style={{
      width: '100vw', minHeight: '100vh',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'var(--bg-main)',
      fontFamily: 'sans-serif',
      position: 'relative',
      padding: '24px 0',
    }}>

      {/* Sign In — top right */}
      <button
        onClick={() => navigate('/login')}
        style={{
          position: 'fixed', top: '20px', right: '24px',
          padding: '11px 26px',
          background: 'var(--btn-gradient)',
          border: 'none', borderRadius: '10px',
          color: '#fff', fontSize: '14px', fontWeight: 600,
          cursor: 'pointer', letterSpacing: '0.3px', zIndex: 10,
        }}
      >
        Sign In
      </button>

      {/* Card */}
      <div style={{
        width: '100%', maxWidth: '580px',
        background: 'var(--bg-card)',
        borderRadius: '28px',
        border: '1px solid rgba(77,124,199,0.2)',
        padding: '64px 72px',
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

        <p style={{ textAlign: 'center', fontSize: '16px', color: 'var(--text-muted)', margin: '0 0 40px' }}>
          Create your account
        </p>

        {/* Success banner */}
        {success && (
          <div style={{
            borderRadius: '12px',
            border: '1px solid rgba(34,197,94,0.3)',
            background: 'rgba(34,197,94,0.08)',
            padding: '18px 20px',
            marginBottom: '28px',
            display: 'flex', gap: '14px', alignItems: 'center',
          }}>
            <MdCheckCircleOutline style={{ width: '24px', height: '24px', color: '#4ade80', flexShrink: 0 }} />
            <div>
              <div style={{ fontSize: '15px', fontWeight: 600, color: '#4ade80', marginBottom: '2px' }}>Account created!</div>
              <div style={{ fontSize: '13px', color: '#86efac' }}>Redirecting to sign in…</div>
            </div>
          </div>
        )}

        {/* Error banner */}
        {errInfo && !success && (
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
              <div style={{ fontSize: '15px', fontWeight: 600, color: '#f87171', marginBottom: '4px' }}>{errInfo.title}</div>
              <div style={{ fontSize: '13px', color: '#fca5a5', lineHeight: 1.5 }}>{errInfo.desc}</div>
            </div>
            <button
              onClick={() => setError('')}
              style={{ background: 'none', border: 'none', color: '#f87171', cursor: 'pointer', fontSize: '22px', lineHeight: 1, flexShrink: 0, padding: '0 0 0 4px' }}
            >×</button>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

          {/* Username */}
          <div>
            <label style={labelStyle}>Username</label>
            <input
              type="text" value={form.username} onChange={set('username')}
              required placeholder="ex: john_doe"
              style={inputStyle}
            />
          </div>

          {/* Email */}
          <div>
            <label style={labelStyle}>Email</label>
            <input
              type="email" value={form.email} onChange={set('email')}
              required placeholder="ex: john@company.com"
              style={inputStyle}
            />
          </div>

          {/* Password */}
          <div>
            <label style={labelStyle}>Password</label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={form.password} onChange={set('password')}
                required placeholder="••••••••"
                style={{ ...inputStyle, paddingRight: '56px' }}
              />
              <button
                type="button" tabIndex={-1}
                onClick={() => setShowPassword(!showPassword)}
                style={{ position: 'absolute', right: '14px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '4px' }}
              >
                {showPassword ? <FaEyeSlash size={18} /> : <FaEye size={18} />}
              </button>
            </div>
          </div>

          {/* Confirm Password */}
          <div>
            <label style={labelStyle}>Confirm Password</label>
            <div style={{ position: 'relative' }}>
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={form.confirmPassword} onChange={set('confirmPassword')}
                required placeholder="••••••••"
                style={{
                  ...inputStyle,
                  paddingRight: '56px',
                  borderColor: form.confirmPassword && form.confirmPassword !== form.password
                    ? 'rgba(220,38,38,0.5)'
                    : 'rgba(77,124,199,0.25)',
                }}
              />
              <button
                type="button" tabIndex={-1}
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                style={{ position: 'absolute', right: '14px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)', padding: '4px' }}
              >
                {showConfirmPassword ? <FaEyeSlash size={18} /> : <FaEye size={18} />}
              </button>
            </div>
            {form.confirmPassword && form.confirmPassword !== form.password && (
              <div style={{ fontSize: '12px', color: '#f87171', marginTop: '8px' }}>Passwords do not match</div>
            )}
          </div>

          

          {/* Submit */}
          <button
            type="submit" disabled={loading || success}
            style={{
              width: '100%', padding: '19px',
              background: 'var(--btn-gradient)',
              border: 'none', borderRadius: '12px',
              color: '#fff', fontSize: '17px', fontWeight: 600,
              cursor: loading || success ? 'not-allowed' : 'pointer',
              opacity: loading || success ? 0.7 : 1,
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
              marginTop: '8px', letterSpacing: '0.3px',
            }}
          >
            {loading
              ? <><FaSpinner size={19} style={{ animation: 'spin 1s linear infinite' }} /> Creating account…</>
              : 'Create Account'}
          </button>

          {/* Already have an account */}
          <p style={{ textAlign: 'center', fontSize: '14px', color: 'var(--text-muted)', margin: 0 }}>
            Already have an account?{' '}
            <span
              onClick={() => navigate('/')}
              style={{ color: 'var(--accent-blue, #4d7cc7)', fontWeight: 600, cursor: 'pointer' }}
            >
              Sign in
            </span>
          </p>

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

export default Signup;
import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { http } from '../../../api/http';

/**
 * 회원 정보 수정 페이지
 * - 기업회원과 지원자 모두 사용
 * - ID, 이름, 성별, 생일은 변경 불가 (읽기 전용)
 * - 비밀번호는 필수 입력 (본인 확인용)
 * - 휴대폰 번호 변경 시 재인증 필요
 */
export default function UserEditPage() {
  const navigate = useNavigate();
  
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  
  // 원본 데이터 (변경 여부 비교용)
  const [originalData, setOriginalData] = useState(null);
  
  // 폼 데이터
  const [formData, setFormData] = useState({
    userid: '',
    username: '',
    gender: '',
    birthday: '',
    email: '',
    phone: '',
    marketingOptIn: false,
    password: '', // 본인 확인용 (필수)
  });

  // 비밀번호 변경 관련
  const [showPasswordChange, setShowPasswordChange] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // 휴대폰 인증 관련
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [otpStatus, setOtpStatus] = useState('');
  const [sendingOtp, setSendingOtp] = useState(false);
  const [verifyingOtp, setVerifyingOtp] = useState(false);

  const [errors, setErrors] = useState({});

  // 휴대폰 번호가 변경되었는지 확인
  const isPhoneChanged = () => {
    if (!originalData) return false;
    const originalPhone = (originalData.phone || '').replace(/[^0-9]/g, '');
    const currentPhone = (formData.phone || '').replace(/[^0-9]/g, '');
    return currentPhone !== originalPhone && currentPhone.length > 0;
  };

  // 기존 회원 정보 로드
  useEffect(() => {
    const fetchProfile = async () => {
      try {
        setLoading(true);
        const res = await http.get('/api/user/profile');
        
        if (res.data) {
          const data = {
            userid: res.data.userid || '',
            username: res.data.username || '',
            gender: res.data.gender || '',
            birthday: res.data.birthday || '',
            email: res.data.email || '',
            phone: res.data.phone || '',
            marketingOptIn: res.data.marketingOptIn || false,
            password: '',
          };
          setFormData(data);
          setOriginalData(res.data);
        }
      } catch (err) {
        if (err.response?.status === 401) {
          navigate('/Login');
          return;
        }
        setError('회원 정보를 불러오는데 실패했습니다.');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [navigate]);

  // 휴대폰 번호 변경 시 인증 상태 초기화
  useEffect(() => {
    if (isPhoneChanged()) {
      setPhoneVerified(false);
      setOtpSent(false);
      setOtpCode('');
      setOtpStatus('휴대폰 번호가 변경되었습니다. 재인증이 필요합니다.');
    } else {
      setOtpStatus('');
      setPhoneVerified(true); // 변경되지 않은 경우 인증 필요 없음
    }
  }, [formData.phone, originalData]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handlePasswordChange = (e) => {
    const { name, value } = e.target;
    setPasswordData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // 휴대폰 인증번호 발송
  const sendOtp = async () => {
    const phone = (formData.phone || '').replace(/[^0-9]/g, '');
    if (!/^[0-9]{10,11}$/.test(phone)) {
      setOtpStatus('휴대폰 번호를 확인해주세요. (숫자만 10~11자리)');
      return;
    }

    try {
      setSendingOtp(true);
      setOtpStatus('인증번호 발송 중...');
      
      const res = await http.post('/api/phone/otp/send', { 
        phone, 
        purpose: 'PROFILE_UPDATE' 
      });
      
      if (res.data.ok) {
        setOtpSent(true);
        setOtpStatus('인증번호를 발송했습니다. 문자로 받은 6자리를 입력해주세요.');
      } else {
        setOtpStatus(res.data.message || '인증번호 발송에 실패했습니다.');
      }
    } catch (err) {
      setOtpStatus(err.response?.data?.message || '인증번호 발송에 실패했습니다.');
    } finally {
      setSendingOtp(false);
    }
  };

  // 휴대폰 인증번호 확인
  const verifyOtp = async () => {
    const phone = (formData.phone || '').replace(/[^0-9]/g, '');
    if (!/^[0-9]{6}$/.test(otpCode)) {
      setOtpStatus('인증번호는 6자리 숫자입니다.');
      return;
    }

    try {
      setVerifyingOtp(true);
      
      const res = await http.post('/api/phone/otp/verify', { phone, code: otpCode });
      
      if (res.data.verified) {
        setPhoneVerified(true);
        setOtpStatus('휴대폰 인증이 완료되었습니다.');
      } else {
        setOtpStatus(res.data.message || '인증번호가 일치하지 않습니다.');
      }
    } catch (err) {
      setOtpStatus(err.response?.data?.message || '인증에 실패했습니다.');
    } finally {
      setVerifyingOtp(false);
    }
  };

  const validate = () => {
    const newErrors = {};
    
    // 비밀번호 필수 확인
    if (!formData.password || formData.password.trim() === '') {
      newErrors.password = '본인 확인을 위해 비밀번호를 입력해주세요.';
    }

    // 이메일 형식 확인
    if (formData.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = '올바른 이메일 형식을 입력해주세요.';
    }

    // 휴대폰 번호 변경 시 인증 확인
    if (isPhoneChanged() && !phoneVerified) {
      newErrors.phone = '변경된 휴대폰 번호의 인증을 완료해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);
    
    if (!validate()) return;

    try {
      setSubmitting(true);
      
      const submitData = {
        email: formData.email,
        phone: (formData.phone || '').replace(/[^0-9]/g, ''),
        marketingOptIn: formData.marketingOptIn,
        password: formData.password, // 본인 확인용
      };
      
      const res = await http.put('/api/user/profile', submitData);
      
      if (res.data.ok) {
        setSuccessMessage('회원 정보가 수정되었습니다.');
        // 원본 데이터 업데이트
        setOriginalData({
          ...originalData,
          email: formData.email,
          phone: submitData.phone,
          marketingOptIn: formData.marketingOptIn,
        });
        setFormData(prev => ({ ...prev, password: '' }));
      }
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '회원 정보 수정에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);
    
    if (!passwordData.currentPassword) {
      setError('현재 비밀번호를 입력해주세요.');
      return;
    }
    if (!passwordData.newPassword || passwordData.newPassword.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('새 비밀번호와 확인이 일치하지 않습니다.');
      return;
    }

    try {
      setSubmitting(true);
      
      const res = await http.put('/api/user/password', passwordData);
      
      if (res.data.ok) {
        setSuccessMessage('비밀번호가 변경되었습니다.');
        setPasswordData({
          currentPassword: '',
          newPassword: '',
          confirmPassword: '',
        });
        setShowPasswordChange(false);
      }
    } catch (err) {
      if (err.response?.status === 401) {
        navigate('/Login');
        return;
      }
      setError(err.response?.data?.error || '비밀번호 변경에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const getGenderLabel = (gender) => {
    switch (gender) {
      case 'MALE': return '남성';
      case 'FEMALE': return '여성';
      case 'UNDISCLOSED': return '미공개';
      default: return gender || '-';
    }
  };

  if (loading) {
    return (
      <main className="main">
        <div className="container py-4">
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            <p className="mt-3 text-muted">정보를 불러오는 중...</p>
          </div>
        </div>
      </main>
    );
  }

  return (
    <main className="main">
      {/* 페이지 타이틀 */}
      <div className="page-title" style={{ backgroundColor: '#003300', marginBottom: '30px' }}>
        <div className="container text-center">
          <h1 style={{ color: 'white' }}>회원 정보 수정</h1>
          <nav className="breadcrumbs">
            <div>
              <span className="responsive-span" style={{ color: 'white' }}>
                회원 정보를 확인하고 수정할 수 있습니다.
              </span>
            </div>
          </nav>
        </div>
      </div>

      <div className="container py-4">
        <div className="row justify-content-center">
          <div className="col-md-8 col-lg-8">
            
            {/* 성공 메시지 */}
            {successMessage && (
              <div className="alert alert-success alert-dismissible fade show">
                <i className="bi bi-check-circle me-2"></i>
                {successMessage}
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={() => setSuccessMessage(null)}
                ></button>
              </div>
            )}

            {/* 에러 메시지 */}
            {error && (
              <div className="alert alert-danger alert-dismissible fade show">
                <i className="bi bi-exclamation-triangle me-2"></i>
                {error}
                <button 
                  type="button" 
                  className="btn-close" 
                  onClick={() => setError(null)}
                ></button>
              </div>
            )}

            {/* 회원 정보 수정 폼 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white">
                <h5 className="mb-0">
                  <i className="bi bi-person me-2"></i>
                  기본 정보
                </h5>
              </div>
              <div className="card-body">
                <form onSubmit={handleSubmit}>
                  {/* 아이디 - 읽기 전용 */}
                  <div className="mb-3">
                    <label htmlFor="userid" className="form-label fw-bold">
                      아이디
                    </label>
                    <input
                      type="text"
                      className="form-control bg-light"
                      id="userid"
                      name="userid"
                      value={formData.userid}
                      disabled
                    />
                    <div className="form-text text-muted">아이디는 변경할 수 없습니다.</div>
                  </div>

                  {/* 이름 - 읽기 전용 */}
                  <div className="mb-3">
                    <label htmlFor="username" className="form-label fw-bold">
                      이름
                    </label>
                    <input
                      type="text"
                      className="form-control bg-light"
                      id="username"
                      name="username"
                      value={formData.username}
                      disabled
                    />
                    <div className="form-text text-muted">이름은 변경할 수 없습니다.</div>
                  </div>

                  {/* 성별 - 읽기 전용 */}
                  <div className="mb-3">
                    <label htmlFor="gender" className="form-label fw-bold">
                      성별
                    </label>
                    <input
                      type="text"
                      className="form-control bg-light"
                      id="gender"
                      name="gender"
                      value={getGenderLabel(formData.gender)}
                      disabled
                    />
                    <div className="form-text text-muted">성별은 변경할 수 없습니다.</div>
                  </div>

                  {/* 생년월일 - 읽기 전용 */}
                  <div className="mb-3">
                    <label htmlFor="birthday" className="form-label fw-bold">
                      생년월일
                    </label>
                    <input
                      type="text"
                      className="form-control bg-light"
                      id="birthday"
                      name="birthday"
                      value={formData.birthday || '-'}
                      disabled
                    />
                    <div className="form-text text-muted">생년월일은 변경할 수 없습니다.</div>
                  </div>

                  <hr className="my-4" />

                  {/* 이메일 - 수정 가능 */}
                  <div className="mb-3">
                    <label htmlFor="email" className="form-label fw-bold">
                      이메일
                    </label>
                    <input
                      type="email"
                      className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                      id="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      placeholder="이메일을 입력하세요"
                    />
                    {errors.email && (
                      <div className="invalid-feedback">{errors.email}</div>
                    )}
                  </div>

                  {/* 휴대폰 - 수정 가능 (변경 시 재인증 필요) */}
                  <div className="mb-3">
                    <label htmlFor="phone" className="form-label fw-bold">
                      휴대폰 번호
                    </label>
                    <div className="d-flex gap-2 mb-2">
                      <input
                        type="text"
                        className={`form-control ${errors.phone ? 'is-invalid' : ''}`}
                        id="phone"
                        name="phone"
                        value={formData.phone}
                        onChange={handleChange}
                        placeholder="01012345678"
                        style={{ maxWidth: '200px' }}
                        disabled={phoneVerified && isPhoneChanged()}
                      />
                      {isPhoneChanged() && !phoneVerified && (
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={sendOtp}
                          disabled={sendingOtp || phoneVerified}
                        >
                          {sendingOtp ? '발송 중...' : '인증번호'}
                        </button>
                      )}
                    </div>
                    
                    {/* OTP 입력 영역 */}
                    {isPhoneChanged() && otpSent && !phoneVerified && (
                      <div className="d-flex gap-2 mb-2">
                        <input
                          type="text"
                          className="form-control"
                          value={otpCode}
                          onChange={(e) => setOtpCode(e.target.value)}
                          placeholder="인증번호 6자리"
                          style={{ maxWidth: '200px' }}
                          maxLength={6}
                        />
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={verifyOtp}
                          disabled={verifyingOtp}
                        >
                          {verifyingOtp ? '확인 중...' : '확인'}
                        </button>
                      </div>
                    )}
                    
                    {/* OTP 상태 메시지 */}
                    {otpStatus && (
                      <div className={`form-text ${phoneVerified ? 'text-success' : 'text-danger'}`}>
                        {otpStatus}
                      </div>
                    )}
                    {errors.phone && (
                      <div className="invalid-feedback d-block">{errors.phone}</div>
                    )}
                  </div>

                  {/* 마케팅 수신 동의 */}
                  <div className="mb-3">
                    <div className="form-check">
                      <input
                        className="form-check-input"
                        type="checkbox"
                        id="marketingOptIn"
                        name="marketingOptIn"
                        checked={formData.marketingOptIn}
                        onChange={handleChange}
                      />
                      <label className="form-check-label" htmlFor="marketingOptIn">
                        마케팅 정보 수신 동의 (선택)
                      </label>
                    </div>
                  </div>

                  <hr className="my-4" />

                  {/* 비밀번호 확인 - 필수 */}
                  <div className="mb-4">
                    <label htmlFor="password" className="form-label fw-bold">
                      비밀번호 확인 <span className="text-danger">*</span>
                    </label>
                    <input
                      type="password"
                      className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                      id="password"
                      name="password"
                      value={formData.password}
                      onChange={handleChange}
                      placeholder="본인 확인을 위해 비밀번호를 입력하세요"
                    />
                    {errors.password && (
                      <div className="invalid-feedback">{errors.password}</div>
                    )}
                    <div className="form-text">
                      회원 정보 수정을 위해 현재 비밀번호를 입력해주세요.
                    </div>
                  </div>

                  {/* 저장 버튼 */}
                  <div className="d-flex justify-content-between">
                    <button 
                      type="button" 
                      className="btn btn-outline-secondary"
                      onClick={() => navigate(-1)}
                    >
                      <i className="bi bi-arrow-left me-1"></i>취소
                    </button>
                    
                    <button 
                      type="submit" 
                      className="btn btn-primary"
                      disabled={submitting}
                    >
                      {submitting ? (
                        <span className="spinner-border spinner-border-sm me-1"></span>
                      ) : (
                        <i className="bi bi-check-circle me-1"></i>
                      )}
                      저장하기
                    </button>
                  </div>
                </form>
              </div>
            </div>

            {/* 비밀번호 변경 섹션 */}
            <div className="card shadow-sm mb-4">
              <div className="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 className="mb-0">
                  <i className="bi bi-key me-2"></i>
                  비밀번호 변경
                </h5>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-primary"
                  onClick={() => setShowPasswordChange(!showPasswordChange)}
                >
                  {showPasswordChange ? '닫기' : '변경하기'}
                </button>
              </div>
              
              {showPasswordChange && (
                <div className="card-body">
                  <form onSubmit={handlePasswordSubmit}>
                    <div className="mb-3">
                      <label htmlFor="currentPassword" className="form-label fw-bold">
                        현재 비밀번호 <span className="text-danger">*</span>
                      </label>
                      <input
                        type="password"
                        className="form-control"
                        id="currentPassword"
                        name="currentPassword"
                        value={passwordData.currentPassword}
                        onChange={handlePasswordChange}
                        placeholder="현재 비밀번호를 입력하세요"
                      />
                    </div>

                    <div className="mb-3">
                      <label htmlFor="newPassword" className="form-label fw-bold">
                        새 비밀번호 <span className="text-danger">*</span>
                      </label>
                      <input
                        type="password"
                        className="form-control"
                        id="newPassword"
                        name="newPassword"
                        value={passwordData.newPassword}
                        onChange={handlePasswordChange}
                        placeholder="새 비밀번호를 입력하세요"
                      />
                      <div className="form-text">8자 이상 + 숫자 1개 이상 + 특수문자 1개 이상</div>
                    </div>

                    <div className="mb-4">
                      <label htmlFor="confirmPassword" className="form-label fw-bold">
                        새 비밀번호 확인 <span className="text-danger">*</span>
                      </label>
                      <input
                        type="password"
                        className="form-control"
                        id="confirmPassword"
                        name="confirmPassword"
                        value={passwordData.confirmPassword}
                        onChange={handlePasswordChange}
                        placeholder="새 비밀번호를 다시 입력하세요"
                      />
                    </div>

                    <div className="text-end">
                      <button 
                        type="submit" 
                        className="btn btn-warning"
                        disabled={submitting}
                      >
                        {submitting ? (
                          <span className="spinner-border spinner-border-sm me-1"></span>
                        ) : (
                          <i className="bi bi-key me-1"></i>
                        )}
                        비밀번호 변경
                      </button>
                    </div>
                  </form>
                </div>
              )}
            </div>

          </div>
        </div>
      </div>
    </main>
  );
}

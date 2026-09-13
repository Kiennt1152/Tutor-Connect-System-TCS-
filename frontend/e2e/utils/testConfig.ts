export const testConfig = {
  baseUrl: process.env.E2E_BASE_URL || 'http://localhost:5173',
  apiUrl: process.env.E2E_API_URL || 'http://localhost:8080/api',
  admin: {
    email: process.env.ADMIN_EMAIL || 'admin01@tcs.test',
    password: process.env.ADMIN_PASSWORD || 'Password123!',
    fallbackEmail: 'admin@tcs.vn',
    fallbackPassword: 'ducminh1011',
  },
  client: {
    email: process.env.CLIENT_EMAIL || 'client01@tcs.test',
    password: process.env.CLIENT_PASSWORD || 'Password123!',
    fallbackEmail: 'haehuynh35@gmail.com',
    fallbackPassword: 'ducminh1011',
  },
  tutor: {
    email: process.env.TUTOR_EMAIL || 'tutor01@tcs.test',
    password: process.env.TUTOR_PASSWORD || 'Password123!',
    userId: Number(process.env.TUTOR_ID) || 2,
    fallbackEmail: 'tutor.le@gmail.com',
    fallbackPassword: '12345678',
  },
  unrelatedTutor: {
    email: process.env.UNRELATED_TUTOR_EMAIL || 'tutor05@tcs.test',
    password: process.env.UNRELATED_TUTOR_PASSWORD || 'Password123!',
    userId: Number(process.env.TARGET_TUTOR_ID) || 5,
  },
  targetTutor: {
    email: process.env.TARGET_TUTOR_EMAIL || 'tutor05@tcs.test',
    userId: Number(process.env.TARGET_TUTOR_ID) || 5,
  },
};

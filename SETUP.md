# Tutor Connect System - Setup Guide

## 🔐 Security Notice

**IMPORTANT:** All API keys and secrets have been removed from source code for security. You must configure environment variables before running the application.

## ⚙️ Environment Setup

### 1. Copy Environment Template

```bash
# Root directory
cp .env.example .env

# Backend directory (optional, if using separate backend .env)
cp backend/.env.example backend/.env
```

### 2. Configure Required Variables

Edit `.env` and fill in your actual values:

#### **JWT Secret** (Required)
Generate a secure 256-bit secret:
```bash
# Using OpenSSL
openssl rand -base64 32

# Or use any secure random string generator
```

#### **Google OAuth** (Required for Login)
1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Create OAuth 2.0 Client ID (Web application)
3. Copy the Client ID to `GOOGLE_CLIENT_ID`

#### **Email/SMTP** (Required for OTP, Password Reset)
For Gmail:
1. Enable 2-Step Verification in your Google Account
2. Generate App Password: [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
3. Use the 16-character App Password in `MAIL_PASSWORD`

#### **AI API Keys** (Optional - for chatbot)
- **Gemini API**: Get from [Google AI Studio](https://aistudio.google.com/apikey)
- **Groq API**: Get from [Groq Console](https://console.groq.com/keys)

If not configured, chatbot will use FAQ-only mode (no AI generation).

### 3. Database Setup

```bash
# Create MySQL database
mysql -u root -p
CREATE DATABASE tutorconnectsystem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
exit;
```

Update database credentials in `.env` if different from defaults:
```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tutorconnectsystem
DB_USERNAME=root
DB_PASSWORD=12345
```

### 4. Run Application

#### Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
```

## 🚨 Security Best Practices

1. **Never commit `.env` files** - Already added to `.gitignore`
2. **Rotate API keys regularly** - Especially if exposed
3. **Use different keys for dev/prod** - Keep production keys separate
4. **Revoke compromised keys immediately**:
   - Groq: https://console.groq.com/keys
   - Gemini: https://aistudio.google.com/apikey

## 📝 Troubleshooting

### Application won't start - "JWT_SECRET is required"
- Ensure `.env` file exists and contains `JWT_SECRET`
- Check that the secret is at least 256 bits (32+ characters)

### Email not sending
- Verify SMTP credentials in `.env`
- For Gmail, ensure you're using App Password, not regular password
- Check firewall/antivirus isn't blocking port 587

### Chatbot returns "API error"
- Check `GEMINI_API_KEY` or `GROQ_API_KEY` is set correctly
- Verify API key hasn't been revoked or rate limited
- Chatbot will fallback to FAQ-only mode if AI APIs fail

## 📚 Additional Documentation

- [Demo Test Scenarios](docs/DEMO_TEST_SCENARIOS_BF09_BF10.md)
- [Use Case Documentation](USE_CASE_DOCUMENTATION.md)
- [API Documentation](http://localhost:8080/swagger-ui.html) (after backend starts)

## 🔗 Useful Links

- [Google OAuth Setup](https://console.cloud.google.com/apis/credentials)
- [Gmail App Passwords](https://myaccount.google.com/apppasswords)
- [Gemini API Keys](https://aistudio.google.com/apikey)
- [Groq API Keys](https://console.groq.com/keys)

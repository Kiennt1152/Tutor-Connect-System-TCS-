import { useEffect, useState } from 'react';
import axios from 'axios';

function App() {
  const [message, setMessage] = useState<string>('Đang thử kết nối đến Backend...');

  useEffect(() => {
    // Sử dụng axios đã cài để gọi sang cổng 8080 của Backend
    axios.get('http://localhost:8080/api/hello')
        .then(response => {
          // Nhận chuỗi chữ từ Backend trả về thành công và lưu vào State
          setMessage(response.data);
        })
        .catch(error => {
          console.error("Lỗi kết nối API:", error);
          setMessage('Kết nối thất bại! Hãy kiểm tra lại Backend (Cổng 8080) xem đã chạy chưa.');
        });
  }, []);

  return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100vh', fontFamily: 'Arial, sans-serif' }}>
        <h1 style={{ color: '#333' }}>Kiểm Tra Kết Nối Hệ Thống Đồ Án</h1>
        <div style={{ marginTop: '20px', padding: '15px 30px', borderRadius: '8px', background: '#eef2f7', border: '1px solid #d0d7de' }}>
          <p style={{ fontSize: '1.3rem', color: '#0052cc', fontWeight: 'bold', margin: 0 }}>
            {message}
          </p>
        </div>
      </div>
  );
}

export default App;
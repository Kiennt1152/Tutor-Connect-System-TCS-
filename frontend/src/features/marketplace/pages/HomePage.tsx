import { useEffect, useState } from 'react';
import axiosClient from '../../../shared/api/axiosClient';

function HomePage() {
    const [message, setMessage] = useState('Đang tải...');

    useEffect(() => {
        axiosClient.get('/hello')
            .then(res => setMessage(res.data))
            .catch(() => setMessage('Không kết nối được Backend.'));
    }, []);

    return (
        <div style={{ textAlign: 'center', marginTop: 40 }}>
            <h1>Trang chủ</h1>
            <p style={{ color: '#0052cc', fontWeight: 'bold' }}>{message}</p>
        </div>
    );
}

export default HomePage;

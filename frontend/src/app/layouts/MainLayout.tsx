import { Outlet } from 'react-router-dom';

function MainLayout() {
    return (
        <div>
            <header style={{ padding: 16, background: '#0052cc', color: '#fff' }}>
                TutorConnectSystem
            </header>
            <main style={{ padding: 24 }}>
                <Outlet />
            </main>
        </div>
    );
}

export default MainLayout;
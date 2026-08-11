const fs = require('fs');

function resolveConflict(filepath, resolver) {
    if (!fs.existsSync(filepath)) {
        console.log(`File not found: ${filepath}`);
        return;
    }
    let content = fs.readFileSync(filepath, 'utf8');
    
    // Windows might use \r\n, so we match \r?\n
    const conflictRegex = /<<<<<<< HEAD\r?\n([\s\S]*?)=======\r?\n([\s\S]*?)>>>>>>> origin\/main/g;
    
    let resolved = false;
    content = content.replace(conflictRegex, (match, head, theirs) => {
        resolved = true;
        return resolver(head, theirs);
    });
    
    if (resolved) {
        fs.writeFileSync(filepath, content, 'utf8');
        console.log(`Resolved conflicts in ${filepath}`);
    } else {
        console.log(`No conflicts found in ${filepath}`);
    }
}

// 1. IdentityServiceImplTest.java
resolveConflict('backend/src/test/java/com/tcs/module/identity/service/impl/IdentityServiceImplTest.java', (head, theirs) => {
    return theirs;
});

// 2. HomeNavbar.tsx
resolveConflict('frontend/src/shared/components/HomeNavbar.tsx', (head, theirs) => {
    return theirs;
});

// 3. ProfilePage.tsx
resolveConflict('frontend/src/features/profile/pages/ProfilePage.tsx', (head, theirs) => {
    if (head.includes('APP_ROUTES.support') && theirs.includes('APP_ROUTES.wallet')) {
        return head.replace(')}', '') + theirs;
    }
    return head + theirs;
});

// 4. ProfilePage.css
resolveConflict('frontend/src/features/profile/pages/ProfilePage.css', (head, theirs) => {
    return head + theirs;
});

// 5. CenterPage.tsx
resolveConflict('frontend/src/features/center/pages/CenterPage.tsx', (head, theirs) => {
    return head;
});

// 6. ContractDetailPage.tsx
resolveConflict('frontend/src/features/contract/pages/ContractDetailPage.tsx', (head, theirs) => {
    return head; 
});

// 7. ContractPage.css
resolveConflict('frontend/src/features/contract/pages/ContractPage.css', (head, theirs) => {
    return head + theirs; 
});

// 8. MarketplaceClassDetailPage.tsx
resolveConflict('frontend/src/features/marketplace/pages/MarketplaceClassDetailPage.tsx', (head, theirs) => {
    return head; 
});

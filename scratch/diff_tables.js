const fs = require('fs');

const tdsLines = fs.readFileSync('C:\\Users\\Admin\\.gemini\\antigravity\\brain\\173fa670-2800-4199-b83c-90dfebfa42be\\scratch\\report_4.0_tds.txt', 'utf8').split('\n');
const tdsTables = new Set();
for (let i = 484; i < 2165; i++) {
  const line = tdsLines[i];
  if (line.startsWith('Tables:')) {
    tdsTables.add(line.replace('Tables:', '').trim());
  }
}

// Read JPA entities
const backendEntities = [
  'ai_chat_messages', 'ai_chat_sessions', 'ai_knowledge_chunks', 'ai_query_cache', 'api_keys',
  'application_status_histories', 'audit_logs', 'categories', 'center_request_fee_holds',
  'center_tutor_memberships', 'child_profiles', 'circumvention_events', 'class_assignments',
  'class_students', 'class_termination_requests', 'clients', 'contract_signatures',
  'contract_templates', 'contracts', 'conversation_participants', 'conversations',
  'disputes', 'districts', 'email_otps', 'email_verification_tokens', 'escrow_transactions',
  'faq_entries', 'favorite_tutors', 'financial_journals', 'grades', 'lesson_attendances',
  'lesson_reschedule_requests', 'lessons', 'locations', 'matching_preferences',
  'media_files', 'message_attachments', 'messages', 'notification_preferences',
  'notification_queues', 'notification_templates', 'notifications', 'parent_child_links',
  'password_reset_tokens', 'payment_histories', 'payment_methods', 'payment_release_requests',
  'payment_transactions', 'platform_admins', 'provinces', 'qualifications',
  'recommendation_logs', 'recruitment_applications', 'recruitment_posts', 'refund_requests',
  'reports', 'reputation_histories', 'reviews', 'schedule_slots', 'subjects',
  'support_tickets', 'system_parameters', 'ticket_messages', 'tutor_applications',
  'tutor_availabilities', 'tutor_busy_times', 'tutor_centers', 'tutor_certificates',
  'tutor_educations', 'tutor_experiences', 'tutor_replacement_requests', 'tutor_subjects',
  'tutoring_classes', 'tutors', 'user_penalties', 'users', 'verification_documents',
  'verification_histories', 'verification_requests', 'wallets', 'wards', 'withdrawal_requests'
];

console.log('Tables in TDS 3.1 count:', tdsTables.size);
console.log('Entities in backend count:', backendEntities.length);

const missingInTDS = backendEntities.filter(t => !tdsTables.has(t));
const missingInBackend = Array.from(tdsTables).filter(t => !backendEntities.includes(t));

console.log('\n--- In Backend but MISSING from TDS 3.1 (' + missingInTDS.length + ') ---');
missingInTDS.forEach(t => console.log(' - ' + t));

console.log('\n--- In TDS 3.1 but NOT a backend JPA Entity (' + missingInBackend.length + ') ---');
missingInBackend.forEach(t => console.log(' - ' + t));

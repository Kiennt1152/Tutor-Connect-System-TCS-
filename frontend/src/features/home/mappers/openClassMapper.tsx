import type { OpenClassApiResponse, OpenClassItem } from '../types/openClassTypes';

export function mapOpenClassItem(item: OpenClassApiResponse): OpenClassItem {
  return {
    id: String(item.classId),
    title: item.title,
    description: item.description,
    creatorName: item.creatorName,
    subjectName: item.subjectName,
    gradeName: item.gradeName,
    lessonMode: item.lessonMode,
    numberOfSessions: item.numberOfSessions,
    budget: Number(item.budget),
    tuitionFee: Number(item.tuitionFee),
    status: item.status,
    createdAt: item.createdAt,
  };
}

export type OpenClassItem = {
  id: string;
  title: string;
  description: string;
  creatorName: string;
  subjectName: string | null;
  gradeName: string | null;
  lessonMode: string;
  numberOfSessions: number;
  budget: number;
  tuitionFee: number;
  status: string;
  createdAt: string | null;
};

export type OpenClassApiResponse = {
  classId: number;
  title: string;
  description: string;
  creatorName: string;
  subjectName: string | null;
  gradeName: string | null;
  lessonMode: string;
  numberOfSessions: number;
  budget: number;
  tuitionFee: number;
  status: string;
  createdAt: string | null;
};

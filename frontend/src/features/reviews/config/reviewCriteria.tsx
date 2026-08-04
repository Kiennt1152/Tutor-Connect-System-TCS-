export type CriterionLevel = {
  score: number;
  label: string;
};

export type ReviewCriterionConfig = {
  code: string;
  question: string;
  levels: CriterionLevel[];
};

export const REVIEW_CRITERIA: ReviewCriterionConfig[] = [
  {
    code: 'punctuality',
    question: 'Gia sư có dạy đúng giờ không?',
    levels: [
      { score: 1, label: 'Thường xuyên trễ' },
      { score: 2, label: 'Đôi khi trễ' },
      { score: 3, label: 'Dạy bình thường' },
      { score: 4, label: 'Đúng giờ' },
      { score: 5, label: 'Luôn luôn đúng giờ' },
    ],
  },
  {
    code: 'clarity',
    question: 'Gia sư giảng bài có dễ hiểu không?',
    levels: [
      { score: 1, label: 'Rất khó hiểu' },
      { score: 2, label: 'Hơi khó hiểu' },
      { score: 3, label: 'Bình thường' },
      { score: 4, label: 'Dễ hiểu' },
      { score: 5, label: 'Rất dễ hiểu' },
    ],
  },
  {
    code: 'dedication',
    question: 'Gia sư có nhiệt tình, tận tâm không?',
    levels: [
      { score: 1, label: 'Thờ ơ' },
      { score: 2, label: 'Ít quan tâm' },
      { score: 3, label: 'Bình thường' },
      { score: 4, label: 'Nhiệt tình' },
      { score: 5, label: 'Rất tận tâm' },
    ],
  },
  {
    code: 'preparation',
    question: 'Gia sư có chuẩn bị bài giảng chu đáo không?',
    levels: [
      { score: 1, label: 'Không chuẩn bị' },
      { score: 2, label: 'Sơ sài' },
      { score: 3, label: 'Bình thường' },
      { score: 4, label: 'Chu đáo' },
      { score: 5, label: 'Rất chu đáo' },
    ],
  },
  {
    code: 'effectiveness',
    question: 'Bạn có hài lòng về sự tiến bộ sau khóa học không?',
    levels: [
      { score: 1, label: 'Không tiến bộ' },
      { score: 2, label: 'Tiến bộ ít' },
      { score: 3, label: 'Bình thường' },
      { score: 4, label: 'Tiến bộ rõ rệt' },
      { score: 5, label: 'Tiến bộ vượt bậc' },
    ],
  },
];

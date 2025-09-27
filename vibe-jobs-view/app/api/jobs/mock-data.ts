export const MOCK_JOBS = [
  {
    id: '1',
    title: 'Senior Backend Engineer (Java)',
    company: 'Acme Global',
    location: 'Tokyo, JP',
    level: 'Senior',
    postedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 2).toISOString(),
    tags: ['Java', 'Spring Boot', 'Microservices'],
    url: 'https://careers.example.com/jobs/1',
    content:
      'We are looking for a Senior Backend Engineer with strong Java and Spring Boot experience to build scalable microservices.',
  },
  {
    id: '2',
    title: 'Staff Software Engineer - Payments',
    company: 'Globex',
    location: 'Singapore, SG',
    level: 'Staff',
    postedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 7).toISOString(),
    tags: ['Distributed Systems', 'Kubernetes', 'MySQL'],
    url: 'https://careers.example.com/jobs/2',
    content:
      'Lead the design of our next-generation payment processing platform and mentor a team of engineers.',
  },
  {
    id: '3',
    title: 'Senior Platform Engineer',
    company: 'Initech',
    location: 'Shanghai, CN',
    level: 'Senior',
    postedAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 12).toISOString(),
    tags: ['Infra', 'Observability', 'SRE'],
    url: 'https://careers.example.com/jobs/3',
    content: 'Help us evolve our platform infrastructure with a focus on reliability and scalability.',
  },
];

import { Helmet } from 'react-helmet-async';

export const PageTitle = ({ title }: { title: string }) => (
  <Helmet
  defaultTitle='Pantry'
  titleTemplate='%s | Pantry'>
    <title>{title}</title>
  </Helmet>
);
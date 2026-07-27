import React from 'react'
import Layout from './components/Layout'
import Hero from './components/Hero'
import Problem from './components/Problem'
import Solution from './components/Solution'
import Differentiators from './components/Differentiators'
import CTA from './components/CTA'

function App() {
  return (
    <Layout>
      <Hero />
      <Problem />
      <Solution />
      <Differentiators />
      <CTA />
    </Layout>
  )
}

export default App

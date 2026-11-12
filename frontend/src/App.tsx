import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import './App.css'

import Tasks from './tasks.tsx'
import TaskPage from "./TaskPage.tsx";

function App() {
  return (
    <Router>
      <main>
        <Routes>
          <Route path="/" element={<Tasks />} />
          <Route path="/task/:id" element={<TaskPage />} />
        </Routes>
      </main>
    </Router>
  )
}

export default App

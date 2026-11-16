import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import './App.css'

import Tasks from './tasks.tsx'
import TaskPage from "./TaskPage.tsx";
import NewAssignmentPage from "./NewAssignmentPage.tsx";

import profile from './assets/profile.svg'
import assignment from './assets/assignment.svg';
import chat from './assets/chat.svg'

function App() {
  return (
    <Router>
      <nav>
        <img src={profile}/>
        <img src={assignment}/>
        <img src={chat}/>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Tasks />} />
          <Route path="/task/:id" element={<TaskPage />} />
          <Route path="/create" element={<NewAssignmentPage />} />
        </Routes>
      </main>
    </Router>
  )
}

export default App
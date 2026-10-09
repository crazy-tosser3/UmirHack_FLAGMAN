import { TextPushBtn } from "@shared/text-push-btn"
import { FaArrowUp } from "react-icons/fa"

export const Footer = () => {
  return (
    <div className="bg-text text-primary p-4">
      <footer className="content flex gap-4 justify-between items-center">
        <h1 className="">Pantry</h1>
        <TextPushBtn className="bg-primary text-text p-2 rounded-full" onClick={() => scrollTo(0, 0)}>
          <FaArrowUp />
        </TextPushBtn>
      </footer>
    </div>
  )
}
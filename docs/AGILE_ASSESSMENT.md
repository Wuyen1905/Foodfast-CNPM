# Đánh giá áp dụng Agile theo Chương 3

Tài liệu đối chiếu với bài giảng **Ch3. Agile SW Dev**, đặc biệt các slide về Agile Manifesto, XP và Scrum.

## Đánh giá hiện tại

| Nội dung bài giảng | Bằng chứng trong FoodFast | Mức áp dụng |
|---|---|---|
| Phản hồi thay đổi | Phạm vi đổi từ giao bằng mô phỏng sang người giao nội bộ, bỏ cổng shipper và mobile theo phản hồi chủ sản phẩm. | Tốt |
| Incremental delivery | Thực hiện theo increment backend, dashboard, trang khách, trợ lý và dọn code cũ. | Tốt |
| User stories và ưu tiên | Product Backlog có user story, điều kiện chấp nhận, điểm và ưu tiên. | Tốt từ increment này |
| Test-first và test automation | Có JUnit, Mockito, Vitest; một test thất bại đã dẫn tới sửa chuẩn hóa tiếng Việt; CI chạy test và build. Chưa phải mọi chức năng đều viết test trước. | Khá |
| Refactoring | Xóa mô phỏng cũ, tách vòng đời giao hàng và thanh toán, gom state machine. | Tốt |
| Continuous integration | Workflow kiểm tra backend, frontend test và build cho mỗi push hoặc pull request. | Tốt |
| Scrum Product Backlog và Sprint Backlog | Có Product Goal, backlog, sprint goal, task và DoD trong repository. | Khá |
| Daily Scrum và teamwork | Chỉ có nhật ký self-check vì dự án một người. Không có daily meeting nhóm. | Chưa đầy đủ |
| Pair programming và collective ownership | Chưa có bằng chứng. | Chưa áp dụng |
| Customer acceptance | Chủ sản phẩm đã tham gia đổi yêu cầu; demo chấp nhận cuối chưa được ghi. | Một phần |

## Kết luận trung thực

Trước lần nâng cấp này, dự án có cách làm linh hoạt nhưng thiếu bằng chứng quy trình. Hiện tại dự án áp dụng **Scrum-inspired project management** và một số thực hành XP như automated testing, refactoring và continuous integration. Không nên nói “áp dụng Scrum đầy đủ” vì dự án một người chưa có Scrum team, Daily Scrum hay review độc lập.

Khi vấn đáp, cách trình bày thuyết phục nhất là chỉ ra bằng chứng cụ thể trong repository và nêu rõ giới hạn. Điều này phù hợp với tinh thần bài giảng hơn việc gắn nhãn Agile mà không có artifact hoặc increment chạy được.

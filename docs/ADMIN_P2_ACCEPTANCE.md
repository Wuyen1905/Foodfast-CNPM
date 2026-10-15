# FoodFast Admin P2 — Báo cáo vận hành

## Mục tiêu

Quản trị viên theo dõi hiệu quả toàn hệ thống bằng dữ liệu thật từ backend, phát hiện xu hướng hủy đơn và đánh giá chất lượng vận hành của từng nhà hàng.

## Chức năng

- Chọn kỳ báo cáo 7, 30 hoặc 90 ngày theo múi giờ `Asia/Ho_Chi_Minh`.
- Xem số đơn, doanh thu thuần, giá trị đơn trung bình, tỷ lệ hoàn tất và tỷ lệ hủy.
- Xem thời gian xác nhận, chuẩn bị và giao hàng trung bình.
- Xem tỷ lệ giao đúng hẹn trên các đơn có đủ ETA và thời điểm giao thực tế.
- Xem biểu đồ xu hướng đơn/doanh thu và cơ cấu trạng thái.
- So sánh tối đa 10 nhà hàng theo số đơn, doanh thu, tỷ lệ hoàn tất và tỷ lệ hủy.
- Thống kê nguyên nhân hủy đơn.
- Xuất báo cáo CSV có BOM UTF-8 để mở đúng tiếng Việt trong Excel.

## Quy tắc tính

- Doanh thu sử dụng `RevenuePolicy`, không cộng đơn chưa hoàn thành, đơn hủy hoặc số tiền đã hoàn.
- Thời gian xác nhận tính từ lúc tạo đơn tới lúc nhà hàng xác nhận.
- Thời gian chuẩn bị tính từ lúc xác nhận tới trạng thái sẵn sàng; dữ liệu cũ được suy ra từ lần phân công giao đầu tiên nếu chưa có mốc sẵn sàng.
- Thời gian giao tính từ lúc bắt đầu giao tới lúc giao thành công.
- Giao đúng hẹn chỉ tính khi có cả ETA và thời điểm giao thực tế.
- Mốc ngày được tính theo giờ Việt Nam, không phụ thuộc múi giờ máy chủ.

## Tiêu chí nghiệm thu

- API báo cáo chỉ cho phép kỳ 7, 30 hoặc 90 ngày.
- Các ngày không có đơn vẫn xuất hiện trên biểu đồ với giá trị 0.
- Tổng theo nhà hàng khớp với tổng đơn trong kỳ.
- Chuyển kỳ báo cáo tải lại dữ liệu, không dùng số giả ở frontend.
- File CSV chứa đúng dữ liệu đang hiển thị và đọc được tiếng Việt.

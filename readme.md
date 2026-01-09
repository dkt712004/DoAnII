# HỘI CHỨNG CHIA NÃO TRONG XÁC THỰC VÀ PHÂN QUYỀN ĐỐI VỚI CÁC HỆ THỐNG PHÂN TÁN
## MÔ PHỎNG VÀ CÁCH GIẢI QUYẾT
### ĐẶT VẤN ĐỀ
Xử lý phiên và quyền của người dùng là một trong những vấn đề cốt lõi của các hệ thống phân tán (ở đây sẽ mô phỏng trong hệ thống microservice với java spring boot)

### CÁC KỊCH BẢN MÔ PHỎNG
### NHÓM 1 - MỘT SỐ KỊCH BẢN MÔ PHỎNG VỚI QUẢN LÝ PHIÊN
#### KỊCH BẢN 1: MÔ PHỎNG CHIA NÃO TRONG QUẢN LÝ PHIÊN GIỮA SERVICE XÁC THỰC VÀ SERVICE NGHIỆP VỤ 

***Ta sẽ sử dụng AUTH-SERVICE (VIẾT TẮT LÀ AUTH) CÙNG VỚI USER-SEVICE (VIẾT TẮT LÀ USER) TRONG CÁC VÍ DỤ***

**Bối cảnh**: Auth có khả năng xác thực user (so sánh username/password được user nhập vào có đúng với csdl không rồi sinh jwt) User có các bộ giải mã jwt ra các thông tin phiên và dựa vào các thông tin đó để xác định phiên.

**B1**: Auth tiến hành xác thực sinh ra jwt từ các thông tin của user nhập vào -> tạo jwt trả về cho client -> bao gồm thông tin user và thời gian hết hạn của token

**B2**: Client tiến hành dùng jwt được cấp thực hiện các request ở tại Auth và User

**B3**: Client logout ra khỏi hệ thống, session được auth-service xóa và đánh hết hạn.

**B4**: Hacker bắt được token của client và tiếp tục request đến user service -> user service vẫn tiếp tục giải mã được thông tin phiên (chưa hết hạn) - và dùng token để tiếp tục khai thác thông tin

#### KỊCH BẢN 2: MÔ PHỎNG HỘI CHỨNG CHIA NÃO TRONG QUẢN LÝ PHIÊN GIỮA 2 INSTANCE CỦA SERVICE XÁC THỰC (AUTH-SERVICE)

Ta mô phỏng tình huống AUTH-SERVICE được triển khai theo mô hình scale ngang (horizontal scaling), gồm nhiều instance chạy song song phía sau Load Balancer.

**Bối cảnh**:
- AUTH-SERVICE được triển khai với 2 instance:
    - AUTH-INSTANCE-A
    - AUTH-INSTANCE-B
- Hai instance không chia sẻ trạng thái phiên (session state)
- Không đồng bộ blacklist hoặc thông tin revoke token
- Load Balancer phân phối request theo cơ chế round-robin

**B1**:  
Client gửi request đăng nhập (username/password) đến hệ thống.  
Load Balancer chuyển request đến AUTH-INSTANCE-A.  
AUTH-INSTANCE-A xác thực thông tin đăng nhập hợp lệ và sinh JWT.  
JWT chứa thông tin người dùng và thời gian hết hạn token.  
JWT được trả về cho client.

**B2**:  
Client sử dụng JWT để thực hiện các request tiếp theo.  
Các request này có thể được Load Balancer phân phối ngẫu nhiên đến:
- AUTH-INSTANCE-A
- AUTH-INSTANCE-B

Cả hai instance đều xác thực JWT độc lập dựa trên chữ ký và thời gian hết hạn, không cần trao đổi thông tin trạng thái với nhau.

**B3**:  
Client thực hiện logout khỏi hệ thống.  
Request logout được Load Balancer chuyển đến AUTH-INSTANCE-A.  
AUTH-INSTANCE-A tiến hành:
- Xóa session cục bộ, hoặc
- Đưa JWT vào blacklist nội bộ, hoặc
- Đánh dấu token hết hiệu lực trong bộ nhớ cục bộ.

Tại thời điểm này, trạng thái token chỉ bị vô hiệu hóa tại AUTH-INSTANCE-A.

**B4**:  
Client hoặc attacker đã chiếm được JWT tiếp tục gửi request đến hệ thống.  
Load Balancer chuyển request đến AUTH-INSTANCE-B.

AUTH-INSTANCE-B tiến hành:
- Giải mã JWT thành công
- Kiểm tra chữ ký hợp lệ
- Kiểm tra thời gian hết hạn vẫn còn hiệu lực
- Không có thông tin token đã bị revoke

Request được chấp nhận là hợp lệ và tiếp tục được xử lý.

**Hiện tượng chia não**:  
Cùng một JWT, cùng một thời điểm:
- AUTH-INSTANCE-A xác định token đã bị vô hiệu hóa
- AUTH-INSTANCE-B xác định token vẫn còn hiệu lực

Hệ thống rơi vào trạng thái nhận thức không nhất quán về phiên người dùng.

**Hậu quả**:
- Logout không có hiệu lực toàn hệ thống
- Token bị đánh cắp vẫn có thể tiếp tục sử dụng
- Người dùng tưởng đã đăng xuất nhưng phiên vẫn tồn tại
- Gia tăng rủi ro bảo mật và vi phạm nguyên tắc nhất quán trạng thái

**Bản chất vấn đề**:  
JWT là cơ chế xác thực không lưu trạng thái (stateless), trong khi việc thu hồi token (revoke) lại phụ thuộc vào trạng thái.  
Khi trạng thái này không được chia sẻ hoặc đồng bộ giữa các instance, hội chứng chia não xảy ra.


#### KỊCH BẢN 3: MÔ PHỎNG HỘI CHỨNG CHIA NÃO TRONG QUẢN LÝ QUYỀN KHI AUTH-SERVICE THAY ĐỔI QUYỀN TRONG PHIÊN NHƯNG USER-SERVICE VẪN NHẬN QUYỀN CŨ

Kịch bản này mô phỏng tình huống quyền của người dùng bị thay đổi trong khi phiên đăng nhập vẫn còn hiệu lực, nhưng sự thay đổi này không được phản ánh đồng bộ giữa các service.

**Bối cảnh**:
- Hệ thống gồm:
    - AUTH-SERVICE: xác thực và quản lý quyền người dùng
    - USER-SERVICE: service nghiệp vụ, kiểm soát truy cập dựa trên quyền
- Quyền (role/permission) được nhúng trực tiếp trong JWT
- JWT có thời gian sống dài
- Không có cơ chế thu hồi hoặc làm mới token khi quyền thay đổi

**B1**:  
Người dùng đăng nhập vào hệ thống.  
AUTH-SERVICE xác thực thành công và sinh JWT.  
JWT chứa thông tin:
- userId
- role = ADMIN
- permissions tương ứng
- thời gian hết hạn token

JWT được trả về cho client.

**B2**:  
Client sử dụng JWT để gọi các API nghiệp vụ tại USER-SERVICE.  
USER-SERVICE giải mã JWT, đọc role = ADMIN và cho phép truy cập các chức năng quản trị.

**B3**:  
Trong khi phiên vẫn còn hiệu lực, quản trị viên hệ thống thay đổi quyền của người dùng:
- Từ ADMIN xuống USER
- Hoặc thu hồi một số permission quan trọng

AUTH-SERVICE cập nhật quyền mới trong cơ sở dữ liệu.  
Tuy nhiên:
- JWT cũ không bị revoke
- Token không bị đánh hết hạn
- Không có thông báo đồng bộ quyền đến USER-SERVICE

**B4**:  
Client tiếp tục sử dụng JWT cũ để gửi request đến USER-SERVICE.  
USER-SERVICE:
- Giải mã JWT thành công
- Đọc role = ADMIN từ token
- Không kiểm tra lại quyền tại AUTH-SERVICE

Request tiếp tục được xử lý với quyền ADMIN.
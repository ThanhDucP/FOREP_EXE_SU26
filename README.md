# FOREP EXE

Nen tang giao viec, theo doi khoi luong cong viec va goi y nguoi nhan viec bang AI cho doanh nghiep nho.

## Trong tam MVP

1. Workspace: mot khong gian lam viec don gian cho cua hang/cong ty nho.
2. OWNER va EMPLOYEE: khong co vai tro quan ly trung gian, nhom hoac phong ban trong MVP.
3. Task: tao viec, giao viec, cap nhat tien do.
4. Workload: xem ai qua tai, ai ranh, ai tre han.
5. Goi y nguoi nhan viec: he thong de xuat nguoi phu hop, OWNER quyet dinh cuoi cung.

Voice khong nam trong MVP dau tien.

## Kien truc hien tai

```text
Backend API Service -> AI Service
```

Front-end cu da duoc go khoi repo. Khi tao front-end moi, khong goi AI service truc tiep; front-end chi goi Backend API Service.

## Cau truc thu muc

```text
backend/     Spring Boot 3 API
ai-service/  FastAPI service cho AI recommendation/summary
docs/        Tai lieu san pham, API, kien truc va dac ta front-end
```

Dac ta chi tiet de xay front-end moi nam tai `docs/frontend-build-spec.md`.

## Chay backend

```bash
cd backend
mvn spring-boot:run
```

Backend mac dinh chay tai `http://localhost:8080`.

## Chay backend + AI bang Docker

```bash
docker compose up --build
```

Local URL:

- Backend: `http://localhost:8080`
- AI Service: `http://localhost:8000`
- PostgreSQL: `localhost:5432`

Neu front-end moi chay o origin khac `http://localhost:5173`, dat bien `CORS_ALLOWED_ORIGINS` truoc khi chay backend.

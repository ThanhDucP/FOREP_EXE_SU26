alter table users
    add column job_title varchar(120);

alter table users
    add column seniority_level varchar(30);

alter table users
    add column skill_rating integer;

alter table users
    add column years_of_experience integer;

alter table users
    add column skills text;

alter table users
    add constraint users_skill_rating_range check (skill_rating is null or skill_rating between 1 and 5);

alter table users
    add constraint users_years_of_experience_non_negative check (years_of_experience is null or years_of_experience >= 0);

drop table if exists doore.member;
drop table if exists doore.team;
drop table if exists doore.study;
drop table if exists doore.curriculum_item;
drop table if exists doore.participant_curriculum_item;
drop table if exists doore.member_team;
drop table if exists doore.participant;
drop table if exists doore.garden;
drop table if exists doore.study_role;
drop table if exists doore.team_role;
drop table if exists doore.document;
drop table if exists doore.file;
drop table if exists doore.attendance;

create table member
(
    id         bigint auto_increment primary key,
    name       varchar(255) not null,
    google_id  varchar(255) not null,
    email      varchar(255) not null,
    image_url  varchar(255) not null,
    is_deleted boolean      not null,
    created_at datetime(6),
    updated_at datetime(6)
);

create table team
(
    id          bigint auto_increment primary key,
    name        varchar(255) not null,
    description varchar(255) not null,
    image_url   varchar(255) not null,
    is_deleted  boolean      not null,
    created_at  datetime(6),
    updated_at  datetime(6)
);

create table study
(
    id          bigint auto_increment primary key,
    name        varchar(255) not null,
    description varchar(255) not null,
    start_date  date         not null,
    end_date    date,
    status      varchar(255) not null,
    is_deleted  boolean      not null,
    team_id     bigint       not null,
    crop_id     bigint       not null,
    created_at  datetime(6),
    updated_at  datetime(6)
);

create table curriculum_item
(
    id         bigint auto_increment primary key,
    name       varchar(255) not null,
    item_order int          not null,
    is_deleted boolean      not null,
    study_id   bigint       not null,
    created_at datetime(6),
    updated_at datetime(6)
);

create table participant_curriculum_item
(
    id                 bigint auto_increment primary key,
    is_checked         boolean not null,
    is_deleted         boolean not null,
    curriculum_item_id bigint  not null,
    participant_id     bigint  not null,
    created_at         datetime(6),
    updated_at         datetime(6)
);

create table member_team
(
    id         bigint auto_increment primary key,
    team_id    bigint  not null,
    member_id  bigint  not null,
    is_deleted boolean not null,
    created_at datetime(6),
    updated_at datetime(6)
);

create table participant
(
    id           bigint auto_increment primary key,
    study_id     bigint  not null,
    member_id    bigint  not null,
    is_completed boolean not null,
    is_deleted   boolean not null,
    created_at   datetime(6),
    updated_at   datetime(6)
);

create table garden
(
    id               bigint auto_increment primary key,
    contributed_date date         not null,
    type             varchar(255) not null,
    is_deleted       boolean      not null,
    contribution_id  bigint       not null,
    team_id          bigint       not null,
    member_id        bigint       not null,
    created_at       datetime(6),
    updated_at       datetime(6)
);

-- 수정 날짜: 2024-02-16
create table attendance
(
    id         bigint auto_increment primary key,
    member_id  bigint not null,
    created_at datetime(6),
    updated_at datetime(6)
);

-- 수정 날짜: 2024-02-29
create table document
(
    id          bigint auto_increment primary key,
    name        varchar(255) not null,
    description varchar(255) not null,
    group_type  varchar(255) not null,
    group_id    bigint       not null,
    is_deleted  boolean      not null,
    access_type varchar(255) not null,
    type        varchar(255) not null,
    uploader_id bigint       not null,
    created_at  datetime(6),
    updated_at  datetime(6)
);

create table file
(
    id          bigint auto_increment primary key,
    url         varchar(255) not null,
    name        varchar(255),
    document_id bigint       not null
);

create table study_role
(
    id          bigint auto_increment primary key,
    study_id    bigint not null,
    role        varchar(45) not null,
    member_id   bigint not null
);

create table team_role
(
    id        bigint auto_increment primary key,
    team_id   bigint      not null,
    role      varchar(45) not null,
    member_id bigint      not null
);

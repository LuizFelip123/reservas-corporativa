-- Execute no MySQL Workbench ou no cliente mysql, usando uma conta com permissão de criar bancos.
-- Este script não apaga dados e não altera a senha de nenhum usuário existente.
CREATE DATABASE IF NOT EXISTS reservas_corporativas
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use uma conta existente com acesso a este banco ou crie uma conta própria no Workbench.
-- Conceda SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX e REFERENCES
-- somente sobre reservas_corporativas.* para a conta da aplicação.
-- Informe essa conta e sua senha no .env.
-- O JPA cria/estende as tabelas usuarios, recursos e reservas ao iniciar.

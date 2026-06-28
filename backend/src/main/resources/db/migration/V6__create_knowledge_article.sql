CREATE TABLE knowledge_article (
                                   id          BIGSERIAL    PRIMARY KEY,
                                   title       VARCHAR(255) NOT NULL,
                                   category    VARCHAR(100) NOT NULL,
                                   content     TEXT         NOT NULL,
                                   created_at  TIMESTAMP    NOT NULL DEFAULT now(),
                                   updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_article_category ON knowledge_article(category);


CREATE TABLE public.parking_parking_payment_methods
(
    parking_id              bigint NOT NULL,
    parking_payment_methods character varying(255)
);

ALTER TABLE ONLY parking_parking_payment_methods
    ADD CONSTRAINT parking_parking_payment_methods_fk FOREIGN KEY (parking_id) REFERENCES parking (id);

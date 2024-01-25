CREATE TABLE public.stop_place_other_transport_modes
(
    stop_place_id              bigint NOT NULL,
    other_transport_modes character varying(255)
);

ALTER TABLE public.stop_place_other_transport_modes
    OWNER to tiamat;

ALTER TABLE ONLY stop_place_other_transport_modes
    ADD CONSTRAINT sp_stop_place_other_transport_modes_fk FOREIGN KEY (stop_place_id) REFERENCES stop_place (id);
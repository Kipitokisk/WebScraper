CREATE TABLE wheel_side (
                            id SERIAL PRIMARY KEY,
                            name TEXT UNIQUE NOT NULL
);

CREATE TABLE nr_of_seats (
                             id SERIAL PRIMARY KEY,
                             name TEXT UNIQUE NOT NULL
);

CREATE TABLE body (
                      id SERIAL PRIMARY KEY,
                      name TEXT UNIQUE NOT NULL
);

CREATE TABLE nr_of_doors (
                             id SERIAL PRIMARY KEY,
                             name TEXT UNIQUE NOT NULL
);

CREATE TABLE engine_capacity (
                                 id SERIAL PRIMARY KEY,
                                 name TEXT UNIQUE NOT NULL
);

CREATE TABLE horsepower (
                            id SERIAL PRIMARY KEY,
                            name TEXT UNIQUE NOT NULL
);

CREATE TABLE petrol_type (
                             id SERIAL PRIMARY KEY,
                             name TEXT UNIQUE NOT NULL
);

CREATE TABLE gears_type (
                            id SERIAL PRIMARY KEY,
                            name TEXT UNIQUE NOT NULL
);

CREATE TABLE traction_type (
                               id SERIAL PRIMARY KEY,
                               name TEXT UNIQUE NOT NULL
);

CREATE TABLE color (
                       id SERIAL PRIMARY KEY,
                       name TEXT UNIQUE NOT NULL
);

CREATE TABLE ad_type (
                         id SERIAL PRIMARY KEY,
                         name TEXT UNIQUE NOT NULL
);

CREATE TABLE particularities (
                                 id SERIAL PRIMARY KEY,
                                 author TEXT,
                                 year_of_fabrication INTEGER,
                                 wheel_side_id INTEGER REFERENCES wheel_side(id),
                                 nr_of_seats_id INTEGER REFERENCES nr_of_seats(id),
                                 body_id INTEGER REFERENCES body(id),
                                 nr_of_doors_id INTEGER REFERENCES nr_of_doors(id),
                                 engine_capacity_id INTEGER REFERENCES engine_capacity(id),
                                 horsepower_id INTEGER REFERENCES horsepower(id),
                                 petrol_type_id INTEGER REFERENCES petrol_type(id),
                                 gears_type_id INTEGER REFERENCES gears_type(id),
                                 traction_type_id INTEGER REFERENCES traction_type(id),
                                 color_id INTEGER REFERENCES color(id)
);

CREATE TABLE cars (
                      id SERIAL PRIMARY KEY,
                      link TEXT UNIQUE NOT NULL,
                      region TEXT,
                      mileage INTEGER,
                      price_eur INTEGER,
                      update_date TIMESTAMP,
                      ad_type_id INTEGER REFERENCES ad_type(id),
                      particularities_id INTEGER REFERENCES particularities(id)
);

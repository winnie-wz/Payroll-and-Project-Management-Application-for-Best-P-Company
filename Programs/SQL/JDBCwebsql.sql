/* Generate paychecks with monthly salary and tax history for all types of employees  */
/* note that hourly employees are contractors who has limited duration and no benefit plans!*/
/* note that in the Salary paychecks/cost/IRS/Project calculations use new start and end times for contractors only*/
/* PART 1. START OF PAYROLL APP   */

DROP view if exists empview;
CREATE VIEW empview AS 
SELECT e.empID, e.name, ea.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, office.phone, office.office_name, ea.dept_code, ea.div_code, ea.projID, 
ea.proj_startdate as start, ea.proj_enddate as end, salary.salary, salary.classification, salary.hourly_pay, 
 project.proj_name 
FROM employeepayroll as e
JOIN employeeassign as ea ON e.empID=ea.empID
JOIN office ON e.office_code =office.office_code 
JOIN project ON ea.projID =project.projID 
JOIN salary ON ea.title=salary.title
;
 
/* Generate paychecks with monthly salary  */
DROP view if exists empsal;
CREATE VIEW empsal AS
SELECT * FROM cs631db.empview 
WHERE empID = '1' AND start_date<='2020-12-31' AND start<='2020-01-01' AND '2020-01-01'<=end;

/* Generate monthly cost reports for all departments   */
DROP view if exists deptsalperm;
CREATE VIEW deptsalperm AS
SELECT d.dept_code,d.dept_name, e.name as manager, e.phone, d.email, count(*) as permEmp, sum(view.hourly_pay) as permSal 
FROM cs631db.empview as view, department as d, empview as e
WHERE view.dept_code= d.dept_code AND view.classification='permanent' AND d.dept_head=e.empID
AND view.start_date<='2020-12-31'AND view.start<='2020-01-01' AND '2020-01-01'<=view.end
AND d.dept_code='1001';

/*Generate monthly cost reports for all departments    */
DROP view if exists deptsalhour;
CREATE VIEW deptsalhour AS
SELECT d.dept_code,d.dept_name, e.name as manager, e.phone, d.email, count(*) as hourEmp, sum(view.hourly_pay) as hourSal 
FROM cs631db.empview as view, department as d, empview as e 
WHERE view.dept_code= d.dept_code AND view.classification='hourly' AND d.dept_head=e.empID
AND view.start_date<='2020-12-31'AND view.start<='2020-01-01' AND '2020-01-01'<=view.end
AND d.dept_code='1001';
/* Generate monthly cost reports for all divisions  */
DROP view if exists divsalperm;
CREATE VIEW divsalperm AS
SELECT d.div_code,d.div_name, e.name as manager, e.phone, d.email, count(*) as permEmp, sum(view.hourly_pay) as permSal 
FROM cs631db.empview as view, division as d, empview as e
WHERE view.div_code= d.div_code AND view.classification='permanent' AND d.div_head=e.empID
AND view.start_date<='2020-12-31'AND view.start<='2020-01-01' AND '2020-01-01'<=view.end
AND d.div_code='FED';
/* Generate monthly cost reports for all divisions  */
DROP view if exists divsalhour;
CREATE VIEW divsalhour AS
SELECT d.div_code,d.div_name, e.name as manager, e.phone, d.email,count(*) as hourEmp, sum(view.hourly_pay) as hourSal 
FROM cs631db.empview as view, division as d, empview as e
WHERE view.div_code= d.div_code AND view.classification='hourly' AND d.div_head=e.empID
AND view.start_date<='2020-12-31'AND view.start<='2020-01-01' AND '2020-01-01'<=view.end
AND d.div_code='FED';

/* Generate IRS tax form for employee #001 in 2020, 2010, 2015, 2018, and hourly emp. #103 partial year */
/* check if full year for IRS report */
DROP view if exists empIRS20;
CREATE VIEW empIRS20 AS
SELECT *, salary as income FROM cs631db.empview 
WHERE empID = '1' AND start_date<='2020-12-31' AND start<='2020-01-01' AND '2020-12-31'<=end;

/* check if project start at last half partial year */
DROP view if exists empIRS20p1;
CREATE VIEW empIRS20p1 AS
SELECT *, hourly_pay*DATEDIFF('2020-12-31', start)/7*40 as income  FROM cs631db.empview 
WHERE empID = '1' AND start_date<='2020-12-31' AND start>='2020-01-01' AND start<='2020-12-31';

/* check if project end at first half partial year */
DROP view if exists empIRS20p2;
CREATE VIEW empIRS20p2 AS
SELECT *, hourly_pay*DATEDIFF(end, '2020-01-01')/7*40  FROM cs631db.empview 
WHERE empID = '1' AND start_date<='2020-12-31' AND end>='2020-01-01' AND end<='2020-12-31';

/* for late join/start, check if hourly at 2011 start at last half partial year */
DROP view if exists empIRS11hr;
CREATE VIEW empIRS11hr AS
SELECT *, hourly_pay*DATEDIFF('2011-12-31',start)/7*40 as income  FROM cs631db.empview 
WHERE empID = '103' AND start_date<='2011-12-31' AND start>='2011-01-01' AND start<='2011-12-31';

/* for early leave, check if hourly at 2018 end at first half partial year */
DROP view if exists empIRS18hr;
CREATE VIEW empIRS18hr AS
SELECT *, hourly_pay*DATEDIFF(end, '2018-01-01')/7*40 as income  FROM cs631db.empview 
WHERE empID = '103' AND start_date<='2018-12-31' AND end>='2018-01-01' AND end<='2018-12-31';

 /* Generate IRS tax forms for all types of employees -see last module  */

/* PART 2. START OF PROJECT MANAGEMENT APP   */

DROP view if exists empview2;
CREATE VIEW empview2 AS 
SELECT e.empID, e.name, ea.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, office.phone, office.office_name, ea.dept_code, ea.div_code, ea.projID, 
ea.proj_startdate as start, ea.proj_enddate as end, salary.salary, salary.classification, salary.hourly_pay, 
 project.proj_name 
FROM employeepayroll as e
JOIN employeeassign as ea ON e.empID=ea.empID
JOIN office ON e.office_code =office.office_code 
JOIN project ON ea.projID =project.projID 
JOIN salary ON ea.title=salary.title
;

/*Track project status and mark up for each project -3 status, completed, ongoing and future   */
DROP view if exists projectSta;
CREATE VIEW  projectSta AS
(SELECT projID, year(end)-year(start) AS span, 'COMPLETED' as status, '100' as milestone FROM project WHERE (CURDATE()>=end AND projID != '0')
UNION
SELECT projID, '0' AS span, 'FUTURE' as status, '0' as milestone FROM project WHERE (CURDATE()<=start AND projID != '0')
UNION
SELECT projID, year(CURDATE())-year(start) AS span, 'ONGOING' as status, null milestone FROM project WHERE (start<=CURDATE() AND CURDATE()<=end AND projID != '0')
);

/*Track man-hour and cost for each emp on the project, note that VP on overhead thus not included and contractor on shorter durations   */
DROP view if exists projectEmpHr;
CREATE VIEW  projectEmpHr AS
(SELECT *, DATEDIFF(end, start)/7*40 AS workhour, DATEDIFF(end, start)/7*40*hourly_pay as charge FROM empview2 WHERE (CURDATE()>=end AND projID != '0')
UNION
SELECT *, '0' AS workhour, '0' as charge FROM empview2 WHERE (CURDATE()<=start AND projID != '0')
UNION
SELECT *, DATEDIFF(end, start)/7*40 AS workhour, DATEDIFF(end, start)/7*40*hourly_pay as charge FROM empview2 WHERE (start<=CURDATE() AND CURDATE()<=end AND projID != '0')
);


/* Track progress statistics, total man-hour and cost on ALL projects   */
DROP view if exists projectProgress;
CREATE VIEW projectProgress AS
SELECT * FROM 
(SELECT p.projID, p.proj_name, p.proj_manager, p.location, p.budget, p.start, p.end, s.span, s.status, e.name, 
COUNT(DISTINCT e.empID) as totalEmp, SUM(eh.charge) as totalCharge, SUM(eh.workhour) as totalworkhour, SUM(eh.charge)/p.budget as prog_milestone
FROM project p, projectSta s, projectEmpHr eh, empview2 e
WHERE p.projID=s.projID AND p.proj_manager=e.empID AND p.projID=eh.projID
GROUP BY p.projID) as results
ORDER BY results.projID ASC;

/* Track progress statistics, total man-hour and cost on each project   */
SELECT * FROM projectProgress WHERE projID='102';

/* Create a new a project (with ID, name, budge, location, duration, etc.)  */

INSERT INTO project VALUES ('112', 'Disney Winter Development', null, 'FL ORLANDO', '10000000', null, null);
/* Assign the project manager and the project team  */
UPDATE project SET proj_manager = '105', start = '2023-1-1', end = '2024-1-1' WHERE projID = '112';
UPDATE employeeassign SET projID = '112', proj_startdate='2023-1-1', proj_enddate='2024-1-1'  WHERE empID = '105';
UPDATE employeeassign SET projID = '112', proj_startdate='2023-1-1', proj_enddate='2024-1-1'  WHERE empID = '7';
UPDATE employeeassign SET projID = '112', proj_startdate='2023-1-1', proj_enddate='2024-1-1'  WHERE empID = '9';
UPDATE employeeassign SET projID = '112', proj_startdate='2023-1-1', proj_enddate='2024-1-1'  WHERE empID = '10';

/* list of all updated projects */
SELECT * FROM 
(SELECT projID, proj_name FROM project WHERE projID>0) 
as p ORDER BY p.projID ASC;

/* list of all updated employees */
SELECT * FROM 
(SELECT ea.empID, ep.name FROM employeeassign ea, employeepayroll ep WHERE ea.empID=ep.empID AND ea.projID is null AND ea.title != 'project manager')
as e ORDER BY e.name ASC;

/* list of all updated managers */
SELECT * FROM (SELECT ea.empID, ep.name FROM employeeassign ea, employeepayroll ep WHERE ea.empID=ep.empID AND ea.projID is null AND ea.title = 'project manager')
as e ORDER BY e.name ASC;


/* Part 1. Last Module to generate IRS for all employees for entire company, first update all empview with new forms */
DROP view if exists empview;
CREATE VIEW empview AS 
SELECT e.empID, e.name, ea.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, office.phone, office.office_name, ea.dept_code, ea.div_code, ea.projID, 
ea.proj_startdate as start, ea.proj_enddate as end, salary.salary, salary.classification, salary.hourly_pay, 
 project.proj_name 
FROM employeepayroll as e
JOIN employeeassign as ea ON e.empID=ea.empID
JOIN office ON e.office_code =office.office_code 
JOIN project ON ea.projID =project.projID 
JOIN salary ON ea.title=salary.title
;

/* generate IRS for all employees work for only one IRS year */
DROP view if exists companyirshr;
CREATE VIEW companyirshr AS 
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, 0 as span, CAST(year(e.start) as signed integer) as IRSyear, (DATEDIFF(e.end,e.start))/7*40*e.hourly_pay as SSincome
FROM empview as e 
where year(e.start)=year(e.end)
UNION
/* generate IRS for all employees work for only two IRS years */
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, 0 as span, CAST(year(e.start)as signed integer) as IRSyear, (DATEDIFF(CONCAT(year(e.start),'-12-31'),start))/7*40*e.hourly_pay  as SSincome
FROM empview as e
where year(e.end)-year(e.start)=1
UNION
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, 0 as span, CAST(year(e.end)as signed integer) as IRSyear, (DATEDIFF(end,CONCAT(year(e.end),'-1-1')))/7*40*e.hourly_pay  as SSincome
FROM empview as e
where year(e.end)-year(e.start)=1
UNION
/* generate IRS for all employees work for >three IRS years */
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, 0 as span, CAST(year(e.start) as signed integer) as IRSyear, (DATEDIFF(CONCAT(year(e.start),'-12-31'),start))/7*40*e.hourly_pay as SSincome
FROM empview as e
where year(e.end)-year(e.start)>1
UNION
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay as unit_pay, 0 as span, CAST(year(e.end) as signed integer)as IRSyear, (DATEDIFF(end,CONCAT(year(e.end),'-1-1')))/7*40*e.hourly_pay as SSincome
FROM empview as e
where year(e.end)-year(e.start)>1
UNION
SELECT e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.salary as unit_pay, (year(e.end)-year(e.start)-1) as span, CAST((year(e.start)+1)as signed integer) as IRSyear, e.salary as SSincome
FROM empview as e
where year(e.end)-year(e.start)>1
;
/*  	Integer taxFed = (int) (empIRSIncome * 0.1);
		Integer taxSta = (int) (empIRSIncome * 0.05);
        Integer taxSS = (int) (empIRSIncome * 0.062);
		Integer medWage=empIRSIncome-taxFed-taxSS;
		Integer taxMedi = (int) (empIRSIncome * 0.0145);
		Integer netWage=empIRSIncome-taxFed-taxSS-taxMedi; */

/*create a raw table of all taxes and salaries for all IRS years*/         
DROP table if exists companyirs;
CREATE table companyirs AS 
SELECT c.empID, c.name, c.address, c.city, c.state, c.zip, c.SSN, c.office_code, c.dept_code, c.div_code, c.classification,
c.span, c.IRSyear, c.SSincome, SSincome*0.1 as FederalTax, SSincome*0.05 as StateTax, SSincome*0.062 as SSTax, SSincome*0.0145 as MedicalTax, SSincome*(1-0.1-0.05-0.062-0.0145) as StateWage
FROM companyirshr as c
WHERE c.start_date<=CURDATE()
;
/* duplicate those rows of multiple year span>0 */
DROP view if exists allirs;
CREATE VIEW allirs as
SELECT * FROM companyirs WHERE span>1;

SELECT * FROM companyirs ORDER BY IRSyear, empID ASC;
INSERT INTO companyirs 
VALUES ('24', 'Bonzo, David', 'vice president', 'DC', 'DC', '20001', '957-98-7369', '6001', '1007', 'COR', 'permanent', '001', '2011', '250000', '25000', '12500', '15500', '3625', '193375')
;

/* this is the end of all SQL queries */


CALL loop_date();

DELIMITER //  
CREATE PROCEDURE loop_date()
BEGIN
DECLARE i INT DEFAULT 0; 
DECLARE span int DEFAULT 0; 
WHILE (i <= e.span) DO
    INSERT INTO companyirs VALUES (e.empID, e.name, e.title, e.start_date, e.address, e.city, e.state, 
e.zip, e.SSN, e.email, e.office_code, e.dept_code, e.div_code, e.classification, e.hourly_pay, year_var, 365) ;
    set year_var = year_var + INTERVAL 1 year;
    SET i = i+1;
END WHILE;
END;
//
DELIMITER ;


/* generate .csv report - not working! */
SELECT *
INTO OUTFILE 'C://Users//tinca//Downloads//CS631_Project//employee.csv'
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
FROM empview
WHERE year(start) = '2018';



